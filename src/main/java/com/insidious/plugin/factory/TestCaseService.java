package com.insidious.plugin.factory;

import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.PageInfo;
import com.insidious.common.weaver.*;
import com.insidious.plugin.callbacks.ClientCallBack;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.client.pojo.ExceptionResponse;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.pojo.*;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.apache.logging.log4j.core.util.NameUtil;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.jcajce.provider.digest.MD5;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

public class TestCaseService {
    private final Logger logger = LoggerUtil.getInstance(TestCaseService.class);
    private final Project project;
    private final VideobugClientInterface client;

    public TestCaseService(Project project, VideobugClientInterface client) {
        this.project = project;
        this.client = client;
    }

    /**
     * try to generate a list of testable methods
     * this one tries to query probe ids based on events METHOD_ENTRY/METHOD_*
     * and then expects the user to do their own figuring out how to do the
     * rest of the flow
     *
     * @param tracePointsCallback callback which receives the results
     * @throws APICallException session related exception
     * @throws IOException      everything is on files
     */
    public void listTestCandidates(ClientCallBack<TracePoint> tracePointsCallback) throws APICallException, IOException {

        List<ExecutionSession> sessions = this.client.fetchProjectSessions().getItems();

        if (sessions.size() == 0) {
            tracePointsCallback.completed();
            return;
        }

        ExecutionSession session = sessions.get(0);

        SearchQuery searchQuery = SearchQuery.ByEvent(List.of(
//                EventType.METHOD_EXCEPTIONAL_EXIT,
//                EventType.METHOD_OBJECT_INITIALIZED,
//                EventType.METHOD_PARAM,
//                EventType.METHOD_THROW,
//                EventType.METHOD_NORMAL_EXIT,
                        EventType.METHOD_ENTRY
                )
        );
        this.client.queryTracePointsByProbe(searchQuery,
                session.getSessionId(), tracePointsCallback);
    }

    /**
     * this is the third attempt to generate test candidates, by using the
     * complete class weave information of a session loaded instead of trying
     * to rebuild the pieces one by one. here we are going to have
     * classInfoList, methodInfoList and also probeInfoList to do *our*
     * processing and then return actually usable results
     *
     * @param clientCallBack callback to stream the results to
     */
    public void

    getTestCandidates(

            ClientCallBack<TestCandidate> clientCallBack

    )


            throws APICallException, IOException, InterruptedException {

        List<ExecutionSession> sessions = this.client.fetchProjectSessions().getItems();

        if (sessions.size() == 0) {
            clientCallBack.completed();
            return;
        }

        ExecutionSession session = sessions.get(0);
        SearchQuery searchQuery = SearchQuery.ByEvent(List.of(
//                EventType.METHOD_EXCEPTIONAL_EXIT,
//                EventType.METHOD_OBJECT_INITIALIZED,
//                EventType.METHOD_PARAM,
//                EventType.METHOD_THROW,
//                EventType.METHOD_NORMAL_EXIT,
                        EventType.METHOD_ENTRY
                )
        );

        BlockingQueue<String> tracePointLock = new ArrayBlockingQueue<>(1);

        List<TracePoint> tracePointList = new LinkedList<>();
        this.client.queryTracePointsByProbe(searchQuery,
                session.getSessionId(), new ClientCallBack<TracePoint>() {
                    @Override
                    public void error(ExceptionResponse errorResponse) {

                    }

                    @Override
                    public void success(Collection<TracePoint> tracePoints) {
                        tracePointList.addAll(tracePoints);

                    }

                    @Override
                    public void completed() {
                        tracePointLock.offer("new ExecutionSession()");
                    }
                });

        tracePointLock.take();

        if (tracePointList.size() == 0) {
            logger.warn("no trace point found for method_entry event");
            clientCallBack.completed();
            return;
        }

        ClassWeaveInfo classWeaveInfo = this.client.getSessionClassWeave(session.getSessionId());

        HashMap<String, Integer> candidateCountMap = new HashMap<>();
        for (TracePoint tracePoint : tracePointList) {

            DataInfo probeInfo = classWeaveInfo.getProbeById(tracePoint.getDataId());
            MethodInfo methodInfo = classWeaveInfo.getMethodInfoById(probeInfo.getMethodId());
            ClassInfo classInfo = classWeaveInfo.getClassInfoById(methodInfo.getClassId());
            String targetName = classInfo.getClassName() + ":" + methodInfo.getMethodName();
            if (!candidateCountMap.containsKey(targetName)) {
                candidateCountMap.put(targetName, 0);
            }

            int newCount = candidateCountMap.get(targetName) + 1;
            if (newCount > 10) {
                continue;
            }
            candidateCountMap.put(targetName, newCount);

            FilteredDataEventsRequest filterDataEventsRequest = tracePoint.toFilterDataEventRequest();

            ReplayData probeReplayData = client.fetchDataEvents(filterDataEventsRequest);

            TestCandidate testCandidate = new TestCandidate(
                    methodInfo, classInfo,
                    tracePoint.getNanoTime(), classWeaveInfo
            );

            TestCandidateMetadata testCandidateMetadata = TestCandidateMetadata.create(
                    methodInfo, tracePoint.getNanoTime(), probeReplayData
            );
            testCandidate.setMetadata(testCandidateMetadata);


            clientCallBack.success(List.of(testCandidate));

            logger.warn("generate test case for ["
                    + classInfo.getClassName() + ":" + methodInfo.getMethodName()
                    + "] using " + probeReplayData.getDataEvents().size() +
                    " events");
        }

        clientCallBack.completed();


    }

    /**
     * If the name doesnt give enough meaning, this will iterate thru all the classes, their
     * methods, and query for each single method_entry probe entry one by one.
     * it is very slow
     *
     * @throws APICallException     it tries to use client
     * @throws IOException          // if something on the disk fails
     * @throws InterruptedException // it waits
     */
    public void listTestCandidatesByEnumeratingAllProbes() throws APICallException, IOException, InterruptedException {

        List<ExecutionSession> sessions = this.client.fetchProjectSessions().getItems();

        if (sessions.size() == 0) {
            return;
        }


        ExecutionSession session = sessions.get(0);
        ClassWeaveInfo classWeaveInfo = this.client.getSessionClassWeave(session.getSessionId());

        for (ClassInfo classInfo : classWeaveInfo.getClassInfoList()) {

            List<MethodInfo> methods =
                    classWeaveInfo.getMethodInfoByClassId(classInfo.getClassId());

            for (MethodInfo method : methods) {


                // test can be generated for public methods
                if (Modifier.isPublic(method.getAccess())) {

                    String methodDescriptor = method.getMethodDesc();
                    List<DataInfo> methodProbes = classWeaveInfo.getProbesByMethodId(method.getMethodId());

                    if (methodProbes.size() == 0) {
                        logger.warn("method [" +
                                classInfo.getClassName() + ":" + method.getMethodName()
                                + "] has no probes");
                        continue;
                    }

                    Optional<DataInfo> methodEntryProbeOption =
                            methodProbes.stream()
                                    .filter(p -> p.getEventType() == EventType.METHOD_ENTRY)
                                    .findFirst();

                    Set<DataInfo> methodParamProbes = methodProbes.stream()
                            .filter(p -> p.getEventType() == EventType.METHOD_PARAM)
                            .collect(Collectors.toSet());

                    assert methodEntryProbeOption.isPresent();

                    DataInfo methodEntryProbe = methodEntryProbeOption.get();

                    BlockingQueue<ExecutionSession> blockingQueue = new ArrayBlockingQueue<>(1);

                    List<TracePoint> tracePoints = new LinkedList<>();
                    client.queryTracePointsByProbe(
                            SearchQuery.ByProbe(List.of(methodEntryProbe.getClassId())),
                            session.getSessionId(), new ClientCallBack<TracePoint>() {
                                @Override
                                public void error(ExceptionResponse errorResponse) {
                                    assert false;
                                }

                                @Override
                                public void success(Collection<TracePoint> tracePointList) {
                                    tracePoints.addAll(tracePointList);
                                }

                                @Override
                                public void completed() {
                                    blockingQueue.offer(new ExecutionSession());
                                }
                            });

                    blockingQueue.take();

                    if (tracePoints.size() == 0) {
                        logger.warn("no trace point found for probe["
                                + methodEntryProbe.getDataId() + " => [" + classInfo.getClassName() + "] => "
                                + method.getMethodName());
                        continue;
                    }

                    TracePoint tracePoint = tracePoints.get(0);

                    FilteredDataEventsRequest filterDataEventsRequest = tracePoint.toFilterDataEventRequest();
                    filterDataEventsRequest.setProbeId(methodEntryProbe.getDataId());
                    ReplayData probeReplayData = client.fetchDataEvents(filterDataEventsRequest);

                    logger.info("generate test case using " + probeReplayData.getDataEvents().size() + " events");


                }


            }


        }

    }


    /**
     * another attempt at generated test candidates but this one tries to get
     * a list of all methods in the session and then expects the user to do
     * their own filtering/sorting. probably not going to be used calling the
     * results as test candidates is misleading
     *
     * @param tracePointsCallback callback to stream results to
     * @throws APICallException from session related operations
     * @throws IOException      every thing is on the files
     */
    public void listTestCandidatesByMethods(
            int typeId,
            ClientCallBack<TestCandidate> tracePointsCallback) throws APICallException, IOException {

        List<ExecutionSession> sessions = this.client.fetchProjectSessions().getItems();

        if (sessions.size() == 0) {
            tracePointsCallback.completed();
            return;
        }

        ExecutionSession session = sessions.get(0);

//        SearchQuery searchQuery = SearchQuery.ByEvent(List.of(
////                EventType.METHOD_EXCEPTIONAL_EXIT,
////                EventType.METHOD_OBJECT_INITIALIZED,
////                EventType.METHOD_PARAM,
////                EventType.METHOD_THROW,
////                EventType.METHOD_NORMAL_EXIT,
//                        EventType.METHOD_ENTRY
//                )
//        );

        this.client.getMethods(session.getSessionId(), typeId, tracePointsCallback);
    }

    @NotNull
    private TracePoint dummyTracePoint() {
        return new TracePoint(
                1,
                2,
                3, 4, 5,
                "file.java",
                "ClassName",
                "ExceptionClassName",
                1234,
                1235);
    }

    public TestSuite generateTestCase(
            Collection<TestCandidate> testCandidateList
    ) throws IOException {

        List<TestCaseUnit> testCases = new LinkedList<>();


        Map<String, List<TestCandidate>> testCandidatesByClass = testCandidateList.stream()
                .collect(Collectors.groupingBy(e -> e.getClassInfo().getClassName()));

        for (String className : testCandidatesByClass.keySet()) {

            List<TestCandidate> candidates = testCandidatesByClass.get(className);
            logger.info("generate test cases for class: "
                    + className + ": we have " + candidates.size() + " candidates");

            Map<String, List<TestCandidate>> candidatesByMethod = candidates.stream().collect(
                    Collectors.groupingBy(e -> e.getMethodInfo().getMethodHash()));

            List<TestMethodScript> testMethods = new LinkedList<>();

            TestCandidateMetadata metadata = null;
            for (String methodHash : candidatesByMethod.keySet()) {
                List<TestCandidate> candidateSet = candidatesByMethod.get(methodHash);


                Set<TestCandidateMetadata> metadataSet = candidateSet.stream()
                        .filter(e -> !e.getMethodInfo().getMethodName().startsWith("<"))
                        .map(TestCandidate::getMetadata)
                        .collect(Collectors.toSet());
                if (metadataSet.size() == 0) {
                    logger.info("class [" + className + "] has only constructor method candidates");
                    continue;
                }
                metadata = metadataSet.stream().findFirst().get();

                MethodSpec.Builder testMethodBuilder = buildTestCaseSkeleton(metadata);

                ByteArrayOutputStream boas = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(boas);

                buildTestFromTestMetadataSet(metadataSet, testMethodBuilder, dos);

                testMethods.add(new TestMethodScript(testMethodBuilder.build(),
                        new MD5Digest(boas.toByteArray()).toString()));


            }

            if (metadata == null) {
                continue;
            }


            String generatedClassName = "Test" + metadata.getUnqualifiedClassname() + "V";
            TypeSpec helloWorld = TypeSpec.classBuilder(generatedClassName)
                    .addModifiers(
                            javax.lang.model.element.Modifier.PUBLIC,
                            javax.lang.model.element.Modifier.FINAL)
                    .addMethods(testMethods
                            .stream()
                            .map(TestMethodScript::getMethodSpec)
                            .collect(Collectors.toList()))
                    .build();

            JavaFile javaFile = JavaFile.builder(metadata.getPackageName(), helloWorld)
                    .build();

            TestCaseUnit testCaseUnit = new TestCaseUnit(javaFile.toString(),
                    metadata.getPackageName(),
                    generatedClassName);


            testCases.add(testCaseUnit);

        }


        return new TestSuite(testCases);

//        BufferedOutputStream boss = new BufferedOutputStream(outputStream);
//        DataOutputStream doss = new DataOutputStream(boss);
//        doss.write("package com.package.test\n".getBytes());
//        doss.write("\t@Test".getBytes());
//        doss.write("\t\tpublic void testMethod1() {\n".getBytes());
//        doss.write("\t}\n".getBytes());


    }

    private void buildTestFromTestMetadataSet(
            Collection<TestCandidateMetadata> metadataCollection,
            MethodSpec.Builder testMethodBuilder, DataOutputStream hashStream) throws IOException {
        assert metadataCollection.size() != 0;

        TestCandidateMetadata metadata = metadataCollection.stream().findFirst().get();

        ClassName assertClass = ClassName.bestGuess("org.junit.Assert");
        Object returnValueSquareClass = null;
        if (metadata.getReturnValueType().startsWith("L")
                || metadata.getReturnValueType().startsWith("[")) {
            returnValueSquareClass = constructClassName(metadata.getReturnValueType());
        } else {
            returnValueSquareClass = getClassFromDescriptor(metadata.getReturnValueType());
        }


        for (TestCandidateMetadata testCandidateMetadata : metadataCollection) {
            @NotNull String parameterString = StringUtil.join(testCandidateMetadata.getParameterValues(), ", ");
            hashStream.write(parameterString.getBytes());

            // return type == V ==> void return type => no return value
            if (testCandidateMetadata.getMethodName().equals("<init>")) {

                ClassName squareClassName = ClassName.get(metadata.getPackageName(),
                        metadata.getUnqualifiedClassname());
                testMethodBuilder.addStatement("$T $L = new $T(" + parameterString + ")",
                        squareClassName, metadata.getTestSubjectInstanceName(), squareClassName);

            } else if (testCandidateMetadata.getReturnValueType().equals("V")) {

                testMethodBuilder.addStatement("$L.$L(" + parameterString + ")",
                        testCandidateMetadata.getTestSubjectInstanceName(), testCandidateMetadata.getMethodName());

            } else {


                testMethodBuilder.addStatement("$T $L = $L.$L(" + parameterString + ")",
                        returnValueSquareClass, testCandidateMetadata.getReturnSubjectInstanceName(),
                        testCandidateMetadata.getTestSubjectInstanceName(), testCandidateMetadata.getMethodName());

                String returnType = testCandidateMetadata.getReturnValueType();

                if (returnType.equals("Ljava/lang/String;")) {
                    hashStream.write(testCandidateMetadata.getReturnValue().getBytes());
                    testMethodBuilder.addStatement("$T.assertEquals($S, $L);",
                            assertClass,
                            testCandidateMetadata.getReturnValue(),
                            testCandidateMetadata.getReturnSubjectInstanceName()
                    );
                } else {
                    hashStream.writeLong(testCandidateMetadata.callReturnProbe().getValue());
                    testMethodBuilder.addStatement("$T.assertEquals($L, $L);",
                            assertClass,
                            testCandidateMetadata.callReturnProbe().getValue(),
                            testCandidateMetadata.getReturnSubjectInstanceName()
                    );
                }


                testMethodBuilder.addComment("");

            }
        }
    }

    @NotNull
    private MethodSpec.Builder buildTestCaseSkeleton(TestCandidateMetadata metadata) {
        MethodSpec.Builder testMethodBuilder =
                MethodSpec.methodBuilder(metadata.getTestMethodName())
                        .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                        .returns(void.class)
                        .addAnnotation(ClassName.get("org.junit", "Test"))
                        .addStatement("/**")
                        .addStatement("$S", "Testing method: " + metadata.getMethodName())
                        .addStatement("$S", "In class: " + metadata.getFullyQualifiedClassname())
                        .addStatement("$S", "Method has " + metadata.getParameterProbes().size() + " " +
                                "parameters.");


        TypeInfo objectTypeInfo = metadata.getReturnTypeInfo();
        testMethodBuilder.addStatement("method returned a value of type: $S",
                objectTypeInfo + " => " + metadata.getReturnValue());

        testMethodBuilder.addStatement("todo: add new variable for each parameter");
        testMethodBuilder.addStatement("**/");
        return testMethodBuilder;
    }

    @NotNull
    private MethodSpec.Builder buildJUnitTestCaseSkeleton(String testMethodName) {
        return MethodSpec.methodBuilder(testMethodName)
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                .returns(void.class)
                .addAnnotation(ClassName.get("org.junit", "Test"));
    }

    private ClassName constructClassName(String methodReturnValueType) {
        char firstChar = methodReturnValueType.charAt(0);
        switch (firstChar) {
            case 'V':
                return ClassName.get(void.class);
            case 'Z':
                return ClassName.get(boolean.class);
            case 'B':
                return ClassName.get(byte.class);
            case 'C':
                return ClassName.get(char.class);
            case 'S':
                return ClassName.get(short.class);
            case 'I':
                return ClassName.get(int.class);
            case 'J':
                return ClassName.get(long.class);
            case 'F':
                return ClassName.get(float.class);
            case 'D':
                return ClassName.get(double.class);
            case 'L':
                String returnValueClass = methodReturnValueType.substring(1).split(";")[0];
                return ClassName.bestGuess(returnValueClass.replace("/", "."));
            case '[':
                String returnValueClass1 = methodReturnValueType.substring(1);
                return ClassName.bestGuess(returnValueClass1.replace("/", ".") + "[]");

            default:
                assert false;

        }
        return null;
    }

    private Integer[] x;

    private Class<?> getClassFromDescriptor(String descriptor) {
        char firstChar = descriptor.charAt(0);
        switch (firstChar) {
            case 'V':
                return void.class;
            case 'Z':
                return boolean.class;
            case 'B':
                return byte.class;
            case 'C':
                return char.class;
            case 'S':
                return short.class;
            case 'I':
                return int.class;
            case 'J':
                return long.class;
            case 'F':
                return float.class;
            case 'D':
                return double.class;
        }
        return null;

    }

    private String upperInstanceName(String methodName) {
        return methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
    }

    private String lowerInstanceName(String methodName) {
        return methodName.substring(0, 1).toLowerCase() + methodName.substring(1);
    }


    private void checkProgressIndicator(String text1, String text2) {
        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                throw new ProcessCanceledException();
            }
            if (text2 != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2(text2);
            }
            if (text1 != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText(text1);
            }
        }
    }


    public TestSuite generateTestCase(
            List<String> classNameList,
            List<ObjectsWithTypeInfo> allObjects
    ) throws APICallException, IOException {

        Map<Long, List<ObjectsWithTypeInfo>> objectsByType =
                allObjects.stream()
                        .collect(Collectors.groupingBy(e -> e.getTypeInfo().getTypeId()));

        List<MethodSpec> testCaseScripts = new LinkedList<>();
        checkProgressIndicator("Generating test cases using " + allObjects.size() + " objects",
                null);

        HashMap<String, Boolean> doneSignatures = new HashMap<>();
        List<TestCaseUnit> testCases = new LinkedList<>();
        for (Long typeId : objectsByType.keySet()) {

            List<ObjectsWithTypeInfo> objectsList = objectsByType.get(typeId);
            TypeInfo typeInfo = objectsList.get(0).getTypeInfo();
            String fullClassName = typeInfo.getTypeNameFromClass();
            if (!classNameList.contains(fullClassName)) {
                continue;
            }
            List<String> classNameParts = new LinkedList<>(Arrays.asList(fullClassName.split("\\.")));
            String simpleClassName = classNameParts.get(classNameParts.size() - 1);
            classNameParts.remove(classNameParts.size() - 1);
            @NotNull String packageName = StringUtil.join(classNameParts, ".");

            int i = 0;
            int total = objectsList.size();
            for (ObjectsWithTypeInfo testableObject : objectsList) {
                i++;
                long objectId = testableObject.getObjectInfo().getObjectId();
                assert objectId != 0;
                checkProgressIndicator("Generating test case for object [ " + i + "/" + total +
                                " ]",
                        null);

                TestMethodScript classTestSuite = generateTestCaseFromObjectHistory(objectId);
                String testHash = classTestSuite.getTestSignature();
                if (doneSignatures.containsKey(testHash)) {
                    continue;
                }
                doneSignatures.put(testHash, true);
                testCaseScripts.add(classTestSuite.getMethodSpec());

            }

            checkProgressIndicator(null, "Generate java source for test scenario");
            String generatedTestClassName = "Test" + simpleClassName + "V";
            TypeSpec helloWorld = TypeSpec.classBuilder(generatedTestClassName)
                    .addModifiers(javax.lang.model.element.Modifier.PUBLIC,
                            javax.lang.model.element.Modifier.FINAL)
                    .addMethods(testCaseScripts)
                    .build();

            JavaFile javaFile = JavaFile.builder(packageName, helloWorld)
                    .build();

            TestCaseUnit testCaseUnit = new TestCaseUnit(
                    javaFile.toString(), packageName, generatedTestClassName);

            testCases.add(testCaseUnit);

        }


        checkProgressIndicator(null, "Generated" + testCases.size() + " test cases");
        return new TestSuite(testCases);


    }

    private TestMethodScript generateTestCaseFromObjectHistory(final Long objectId) throws APICallException,
            IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        PageInfo pagination = new PageInfo(0, 500000, PageInfo.Order.ASC);

        FilteredDataEventsRequest request = new FilteredDataEventsRequest();
        request.setPageInfo(pagination);
        request.setObjectId(objectId);
        ReplayData objectReplayData = client.
                fetchObjectHistoryByObjectId(request);


        final Map<String, ObjectInfo> objectInfoMap = objectReplayData.getObjectInfo();

        List<DataEventWithSessionId> objectEvents = objectReplayData.getDataEvents();


        PageInfo paginationOlder = new PageInfo(0, 100, PageInfo.Order.DESC);
        DataEventWithSessionId event = objectEvents.get(0);
        FilteredDataEventsRequest filterRequest = new FilteredDataEventsRequest();
//        (long) -1, event.getThreadId(), event.getNanoTime(), paginationOlder
        filterRequest.setThreadId(event.getThreadId());
        filterRequest.setNanotime(event.getNanoTime());
        filterRequest.setPageInfo(paginationOlder);
        ReplayData callContext = client.fetchObjectHistoryByObjectId(filterRequest);


        List<DataEventWithSessionId> contextEvents = callContext.getDataEvents();
        Collections.reverse(contextEvents);
        contextEvents.remove(contextEvents.size() - 1);
        objectReplayData.getDataEvents().addAll(0, contextEvents);


        List<DataEventWithSessionId> objectEventsReverse =
                new ArrayList<>(objectReplayData.getDataEvents());
        Collections.reverse(objectEventsReverse);
        objectReplayData.setDataEvents(objectEventsReverse);

        // iterate from oldest to newest event
        final Map<String, TypeInfo> typeInfoMap = objectReplayData.getTypeInfo();
        final Map<String, DataInfo> probeInfoMap = objectReplayData.getDataInfoMap();
        final Map<String, ClassInfo> classInfoMap = objectReplayData.getClassInfoMap();
        final Map<String, MethodInfo> methodInfoMap = objectReplayData.getMethodInfoMap();

        final ObjectInfo subjectObjectInfo =
                objectInfoMap.get(String.valueOf(objectId));
        final TypeInfo subjectTypeInfo =
                typeInfoMap.get(String.valueOf(subjectObjectInfo.getTypeId()));


        MethodSpec.Builder testMethodBuilder =
                buildJUnitTestCaseSkeleton("testClassAsInstance" + objectId);
        checkProgressIndicator("Parsing " + objectEvents.size() + " events of object[" + objectId +
                "]", null);


        int eventIndex = -1;
        TypeInfo typeInfo = null;
        TestCandidateMetadata testCandidateMetadata = null;

        Map<String, Boolean> variableStack = new HashMap<>();
        for (DataEventWithSessionId dataEvent : objectEvents) {
            eventIndex++;
            final long eventValue = dataEvent.getValue();
            String eventValueString = String.valueOf(eventValue);
            ObjectInfo objectInfo = objectInfoMap.get(eventValueString);

            String objectInfoString = "";
            TypeInfo objectTypeInfo = null;
            if (objectInfo != null) {
                long objectTypeId = objectInfo.getTypeId();
                objectTypeInfo = typeInfoMap.get(String.valueOf(objectTypeId));
                objectInfoString = "[Object:" + objectInfo.getObjectId() + "]";
                typeInfo = typeInfoMap.get(String.valueOf(objectInfo.getTypeId()));
            }

            DataInfo probeInfo = probeInfoMap.get(String.valueOf(dataEvent.getDataId()));
            int methodId = probeInfo.getMethodId();

            int callStack = 0;
            ClassInfo currentClassInfo = classInfoMap.get(String.valueOf(probeInfo.getClassId()));

            switch (probeInfo.getEventType()) {
                case CALL:

                    String constructorOwnerClass = probeInfo.getAttribute("Owner", "");
                    if (!StringUtil.isEmpty(constructorOwnerClass)) {
                        constructorOwnerClass = constructorOwnerClass.replaceAll("/", ".");
                        if (subjectTypeInfo != null
                                && Objects.equals(subjectTypeInfo.getTypeNameFromClass(),
                                constructorOwnerClass)) {

//                            MethodInfo methodInfo = methodInfoMap.get(String.valueOf(probeInfo.getMethodId()));
                            MethodInfo methodInfo =
                                    getMethodInfo(objectEvents.size() - eventIndex - 1,
                                            objectReplayData);


                            TestCandidateMetadata newTestCaseMetadata = TestCandidateMetadata.create(
                                    methodInfo, dataEvent.getNanoTime(), objectReplayData);

                            if (newTestCaseMetadata.getTestSubjectInstanceName() == null
                                    && testCandidateMetadata != null) {
                                newTestCaseMetadata.setTestSubjectInstanceName(
                                        testCandidateMetadata.getTestSubjectInstanceName());
                            }
                            if (newTestCaseMetadata.getMethodName().equals("<init>")
                                    && variableStack.containsKey(newTestCaseMetadata.getTestSubjectInstanceName())
                            ) {
                                break;
//                                continue;
                            }
                            variableStack.put(newTestCaseMetadata.getTestSubjectInstanceName(),
                                    true);

                            if (newTestCaseMetadata.getCallReturnProbe() == null
                                    || newTestCaseMetadata.getTestSubjectInstanceName() == null) {
                                logger.debug("skipping method_entry, failed to find call return: " + methodInfo + " -> " + dataEvent);
                                continue;
                            }

                            buildTestFromTestMetadataSet(
                                    List.of(newTestCaseMetadata), testMethodBuilder,
                                    dos);
                            testCandidateMetadata = newTestCaseMetadata;


                        }
                    }


                    callStack += 1;
                    break;

            }

        }

        return new TestMethodScript(testMethodBuilder.build(), NameUtil.md5(baos.toString()));

    }

    private MethodInfo getMethodInfo(int eventIndex, ReplayData objectReplayData) {
        int callStack = 0;
        int direction = -1;
        for (int i = eventIndex + direction; i < objectReplayData.getDataEvents().size(); i += direction) {
            DataEventWithSessionId event = objectReplayData.getDataEvents().get(i);
            DataInfo probeInfo = objectReplayData.getDataInfoMap().get(String.valueOf(event.getDataId()));
            switch (probeInfo.getEventType()) {
                case CALL:
                    callStack += 1;
                    break;
                case CALL_RETURN:
                    callStack -= 1;
                case METHOD_ENTRY:
                    if (callStack == 0) {
                        return objectReplayData
                                .getMethodInfoMap()
                                .get(String.valueOf(probeInfo.getMethodId()));
                    }
            }
            if (probeInfo.getEventType() == EventType.CALL) {
                callStack += 1;
            }
        }
        return null;
    }

}
