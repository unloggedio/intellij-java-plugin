package com.insidious.plugin.factory;

import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.weaver.*;
import com.insidious.plugin.callbacks.ClientCallBack;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.VideobugLocalClient;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.client.pojo.ExceptionResponse;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.extension.model.PageInfo;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.pojo.*;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.jetbrains.annotations.NotNull;

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

    public TestCaseService(Project project, VideobugLocalClient client) {
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
                    methodInfo, classInfo, session,
                    probeInfo, classWeaveInfo
            );

            TestCandidateMetadata testCandidateMetadata = TestCandidateMetadata.create(
                    testCandidate, probeReplayData
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

        this.client.getMethods(session.getSessionId(), tracePointsCallback);
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
    )
            throws IOException {

        List<TestCaseUnit> testCases = new LinkedList<>();


        Map<String, List<TestCandidate>> testCandidatesByClass = testCandidateList.stream()
                .collect(Collectors.groupingBy(e -> e.getClassInfo().getClassName()));

        for (String className : testCandidatesByClass.keySet()) {

            List<TestCandidate> candidates = testCandidatesByClass.get(className);
            logger.info("generate test cases for class: "
                    + className + ": we have " + candidates.size() + " candidates");

            Map<String, List<TestCandidate>> candidatesByMethod = candidates.stream().collect(
                    Collectors.groupingBy(e -> e.getMethodInfo().getMethodHash()));

            List<MethodSpec> testMethods = new LinkedList<>();

            TestCandidateMetadata metadata = null;
            for (String methodHash : candidatesByMethod.keySet()) {
                List<TestCandidate> candidateSet = candidatesByMethod.get(methodHash);
                TestCandidate testCandidate = candidateSet.get(0);


                Set<TestCandidateMetadata> metadataSet = candidateSet.stream()
                        .filter(e -> !e.getMethodInfo().getMethodName().startsWith("<"))
                        .map(TestCandidate::getMetadata)
                        .collect(Collectors.toSet());
                if (metadataSet.size() == 0) {
                    logger.info("class [" + className + "] has only constructor method candidates");
                    continue;
                }
                metadata = metadataSet.stream().findFirst().get();
                MethodSpec.Builder testMethodBuilder = buildTestFromTestMetadataSet(metadataSet);

                testMethods.add(testMethodBuilder.build());


            }

            if (metadata == null) {
                continue;
            }


            TypeSpec helloWorld = TypeSpec.classBuilder("Test" + metadata.getUnqualifiedClassname())
                    .addModifiers(javax.lang.model.element.Modifier.PUBLIC,
                            javax.lang.model.element.Modifier.FINAL)
                    .addMethods(testMethods)
                    .build();

            JavaFile javaFile = JavaFile.builder(metadata.getPackageName(), helloWorld)
                    .build();

            TestCaseUnit testCaseUnit = new TestCaseUnit(metadata, javaFile.toString());


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

    @NotNull
    private MethodSpec.Builder buildTestFromTestMetadataSet(Collection<TestCandidateMetadata> metadataCollection) {
        assert metadataCollection.size() != 0;

        TestCandidateMetadata metadata = metadataCollection.stream().findFirst().get();
        MethodSpec.Builder testMethodBuilder = buildTestCaseSkeleton(metadata);

        ClassName squareClassName = ClassName.get(metadata.getPackageName(), metadata.getUnqualifiedClassname());

        testMethodBuilder.addStatement("$T $L = new $T()", squareClassName, metadata.getTestSubjectInstanceName(), squareClassName);

        Object returnValueSquareClass = null;
        if (metadata.getReturnValueType().startsWith("L")
                || metadata.getReturnValueType().startsWith("[")) {
            returnValueSquareClass = constructClassName(metadata.getReturnValueType());
        } else {
            returnValueSquareClass = getClassFromDescriptor(metadata.getReturnValueType());
        }


        for (TestCandidateMetadata testCandidateMetadata : metadataCollection) {
            @NotNull String parameterString = StringUtil.join(testCandidateMetadata.getParameterValues(), ", ");

            // return type == V ==> void return type => no return value
            if (testCandidateMetadata.getReturnValueType().equals("V")) {

                testMethodBuilder.addStatement("$L.$L(" + parameterString + ")",
                        testCandidateMetadata.getTestSubjectInstanceName(), testCandidateMetadata.getMethodName());

            } else {


                testMethodBuilder.addStatement("$T $L = $L.$L(" + parameterString + ")",
                        returnValueSquareClass, testCandidateMetadata.getReturnSubjectInstanceName(),
                        testCandidateMetadata.getTestSubjectInstanceName(), testCandidateMetadata.getMethodName());


                if (testCandidateMetadata.getReturnValue() == null) {
                    testMethodBuilder.addStatement("assert $L == $S",
                            testCandidateMetadata.getReturnSubjectInstanceName(), testCandidateMetadata.callReturnProbe().getValue());
                } else {
                    testMethodBuilder.addStatement("assert $L == $S",
                            testCandidateMetadata.getReturnSubjectInstanceName(), testCandidateMetadata.getReturnValue());
                }


                testMethodBuilder.addStatement("// thats all folks");

            }
        }
        return testMethodBuilder;
    }

    @NotNull
    private MethodSpec.Builder buildTestCaseSkeleton(TestCandidateMetadata metadata) {
        MethodSpec.Builder testMethodBuilder =
                MethodSpec.methodBuilder(metadata.getTestMethodName())
                        .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                        .returns(void.class)
                        .addAnnotation(ClassName.get("org.junit", "Test"))
//                .addParameter(String[].class, "args")
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


    public TestSuite generateTestCase(
            List<ObjectsWithTypeInfo> allObjects
    ) {

        List<TestCaseUnit> testCaseScripts = new LinkedList<>();
        TestSuite testSuite = new TestSuite(List.of());
        for (ObjectsWithTypeInfo testableObject : allObjects) {


            long objectId = testableObject.getObjectInfo().getObjectId();
//            ReplayData objectHistory = client.fetchObjectHistoryByObjectId(
//                    objectId, -1, (long) -1, new PageInfo());

            assert objectId != 0;

            TestSuite classTestSuite = generateTestCaseFromObjectHistory(objectId);
            testCaseScripts.addAll(classTestSuite.getTestCaseScripts());

        }

        return new TestSuite(testCaseScripts);


    }

    private TestSuite generateTestCaseFromObjectHistory(final Long objectId) {
        List<TestCaseUnit> testCases = new LinkedList<>();

        ReplayData objectHistory = client.fetchObjectHistoryByObjectId(
                objectId, -1, (long) -1, new PageInfo(0, 50000, PageInfo.Order.ASC));


        Map<String, ObjectInfo> objectInfoMap = objectHistory.getObjectInfo();

        List<DataEventWithSessionId> objectEvents = objectHistory.getDataEvents();

        // iterate from oldest to newest event
        Map<String, TypeInfo> typeInfo = objectHistory.getTypeInfo();
        Map<String, DataInfo> probeInfoMap = objectHistory.getDataInfoMap();

        MethodSpec.Builder objectRepeater = MethodSpec.constructorBuilder();

        for (DataEventWithSessionId dataEvent : objectEvents) {

            final long eventValue = dataEvent.getValue();
            String eventValueString = String.valueOf(eventValue);
            ObjectInfo objectInfo = objectInfoMap.get(eventValueString);

            if (eventValue != objectId) {
                continue;
            }

            String objectInfoString = "";
            TypeInfo objectTypeInfo = null;
            if (objectInfo != null) {
                long objectTypeId = objectInfo.getTypeId();
                objectTypeInfo = typeInfo.get(String.valueOf(objectTypeId));
                objectInfoString = "[Object:" + objectInfo.getObjectId() + "]";

            }

            DataInfo probeInfo = probeInfoMap.get(String.valueOf(dataEvent.getDataId()));
            int methodId = probeInfo.getMethodId();

            logger.warn("Object" +
                    objectInfoString +
                    "[Thread:" + dataEvent.getThreadId() + "]" +
                    "[Seq:" + dataEvent.getNanoTime() + "]" +
                    "[Time:" + dataEvent.getRecordedAt().getTime() + "] -> " +
//                    "[Class:" + objectTypeInfo.getTypeNameFromClass() + "] -> "+
                    probeInfo.getEventType());

        }
        logger.warn(" ============================================================= ");
        logger.warn(" ============================================================= ");
        logger.warn(" ============================================================= ");


        TestSuite testSuite = new TestSuite(testCases);
        return testSuite;
    }


}
