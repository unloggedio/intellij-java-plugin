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
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.squareup.javapoet.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

public class TestCaseService {
    public static final ClassName JUNIT_CLASS_NAME = ClassName.get("org.junit", "Test");
    private final Logger logger = LoggerUtil.getInstance(TestCaseService.class);
    private final Project project;
    private final VideobugClientInterface client;
    final private int MAX_TEST_CASE_LINES = 1000;
    private final CompareMode MODE = CompareMode.SERIALIZED_JSON;

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

    //    public TestSuite generateTestCase(
//            Collection<TestCandidate> testCandidateList
//    ) throws IOException {
//
//        List<TestCaseUnit> testCases = new LinkedList<>();
//
//
//        Map<String, List<TestCandidate>> testCandidatesByClass = testCandidateList.stream()
//                .collect(Collectors.groupingBy(e -> e.getClassInfo().getClassName()));
//
//        for (String className : testCandidatesByClass.keySet()) {
//
//            List<TestCandidate> candidates = testCandidatesByClass.get(className);
//            logger.info("generate test cases for class: "
//                    + className + ": we have " + candidates.size() + " candidates");
//
//            Map<String, List<TestCandidate>> candidatesByMethod = candidates.stream().collect(
//                    Collectors.groupingBy(e -> e.getMethodInfo().getMethodHash()));
//
//            List<TestMethodScript> testMethods = new LinkedList<>();
//
//            TestCandidateMetadata metadata = null;
//            for (String methodHash : candidatesByMethod.keySet()) {
//                List<TestCandidate> candidateSet = candidatesByMethod.get(methodHash);
//
//
//                Set<TestCandidateMetadata> metadataSet = candidateSet.stream()
//                        .filter(e -> !e.getMethodInfo().getMethodName().startsWith("<"))
//                        .map(TestCandidate::getMetadata)
//                        .collect(Collectors.toSet());
//                if (metadataSet.size() == 0) {
//                    logger.info("class [" + className + "] has only constructor method candidates");
//                    continue;
//                }
//                metadata = metadataSet.stream().findFirst().get();
//                Map<String, Boolean> variableStack = new HashMap<>();
//
//                MethodSpec.Builder testMethodBuilder = buildTestCaseSkeleton(metadata);
//
//                ByteArrayOutputStream boas = new ByteArrayOutputStream();
//                DataOutputStream dos = new DataOutputStream(boas);
//
//                buildTestFromTestMetadataSet(metadataSet, variableStack, testMethodBuilder, dos);
//
//                testMethods.add(new TestMethodScript(testMethodBuilder.build(),
//                        new MD5Digest(boas.toByteArray()).toString()));
//
//
//            }
//
//            if (metadata == null) {
//                continue;
//            }
//
//
//            String generatedClassName = "Test" + metadata.getUnqualifiedClassname() + "V";
//            TypeSpec helloWorld = TypeSpec.classBuilder(generatedClassName)
//                    .addModifiers(
//                            javax.lang.model.element.Modifier.PUBLIC,
//                            javax.lang.model.element.Modifier.FINAL)
//                    .addMethods(testMethods
//                            .stream()
//                            .map(TestMethodScript::getMethodSpec)
//                            .collect(Collectors.toList()))
//                    .build();
//
//            JavaFile javaFile = JavaFile.builder(metadata.getPackageName(), helloWorld)
//                    .build();
//
//            TestCaseUnit testCaseUnit = new TestCaseUnit(javaFile.toString(),
//                    metadata.getPackageName(),
//                    generatedClassName);
//
//
//            testCases.add(testCaseUnit);
//
//        }
//
//
//        return new TestSuite(testCases);
//
////        BufferedOutputStream boss = new BufferedOutputStream(outputStream);
////        DataOutputStream doss = new DataOutputStream(boss);
////        doss.write("package com.package.test\n".getBytes());
////        doss.write("\t@Test".getBytes());
////        doss.write("\t\tpublic void testMethod1() {\n".getBytes());
////        doss.write("\t}\n".getBytes());
//
//
//    }
//
    private void buildTestFromTestMetadataSet(ObjectRoutine objectRoutine) throws IOException {
        assert objectRoutine.getMetadata().size() != 0;
        List<TestCandidateMetadata> metadataCollection = objectRoutine.getMetadata();
        VariableContainer variableContainer = objectRoutine.getVariableContainer();


        ClassName assertClass = ClassName.bestGuess("org.junit.Assert");

        for (TestCandidateMetadata testCandidateMetadata : metadataCollection) {

            objectRoutine.addComment("");
            objectRoutine.addComment("Test candidate method ["
                    + testCandidateMetadata.getMethodName()
                    + "] - took " + Long.valueOf(testCandidateMetadata.getCallTimeNanoSecond() / 1000).intValue() + "ms");

            Object returnValueSquareClass = null;
            String returnParameterType = testCandidateMetadata.getReturnParameter().getType();
            if (returnParameterType.startsWith("L") || returnParameterType.startsWith("[")) {

                switch (returnParameterType) {
                    case "Ljava/lang/Integer;":
                        returnValueSquareClass = int.class;
                        break;
                    case "Ljava/lang/Long;":
                        returnValueSquareClass = long.class;
                        break;
                    default:
                        returnValueSquareClass = constructClassName(returnParameterType);
                }

            } else {
                returnValueSquareClass = getClassFromDescriptor(returnParameterType);
            }


            String parameterString = createMethodParametersString(testCandidateMetadata);

            // return type == V ==> void return type => no return value
            Parameter testSubject = testCandidateMetadata.getTestSubject();
            if (testCandidateMetadata.getMethodName().equals("<init>")) {


                ClassName squareClassName = ClassName.get(testCandidateMetadata.getPackageName(),
                        testCandidateMetadata.getUnqualifiedClassname());


                objectRoutine.addStatement("$T $L = new $T(" + parameterString + ")",
                        squareClassName,
                        testSubject.getName(),
                        squareClassName);


            } else {
                Parameter returnParameter = testCandidateMetadata.getReturnParameter();
                testSubject = testCandidateMetadata.getTestSubject();
                String testSubjectName = testSubject.getName();


                if (returnParameter.getType().equals("V")) {

                    objectRoutine.addStatement("$L.$L(" + parameterString + ")",
                            testSubjectName, testCandidateMetadata.getMethodName());

                } else {
                    Object returnValue = returnParameter.getValue();

                    String returnSubjectInstanceName = returnParameter.getName();
                    if (variableContainer.contains(returnSubjectInstanceName)) {
                        objectRoutine.addStatement("$L = $L.$L(" + parameterString + ")",
                                returnSubjectInstanceName,
                                testSubjectName,
                                testCandidateMetadata.getMethodName());

                    } else {
                        objectRoutine.addStatement("$T $L = $L.$L(" + parameterString + ")",
                                returnValueSquareClass, returnSubjectInstanceName,
                                testSubjectName,
                                testCandidateMetadata.getMethodName());
                        variableContainer.add(returnParameter);

                    }


                    String returnType = returnParameter.getType();

                    // deserialize and compare objects
                    byte[] serializedBytes = returnParameter.getProb().getSerializedValue();
                    if (serializedBytes == null) {
                        serializedBytes = new byte[0];
                    }
                    String serializedValue = new String(serializedBytes);
                    objectRoutine.addComment("Serialized value: " + serializedValue);

                    // reconstruct object from the serialized form to an object instance in the
                    // test method to compare it with the new object, or do it the other way
                    // round ? Maybe serializing the object and then comparing the serialized
                    // string forms would be more readable ? string comparison would fail if the
                    // serialization has fields serialized in random order
                    if (serializedBytes.length > 0) {
                        if (MODE == CompareMode.OBJECT) {
                            objectRoutine.addStatement("$T $L = gson.fromJson($S, $T.class)",
                                    returnValueSquareClass,
                                    returnSubjectInstanceName + "Expected",
                                    serializedValue,
                                    returnValueSquareClass
                            );
                            returnValue = returnSubjectInstanceName + "Expected";
                        } else if (MODE == CompareMode.SERIALIZED_JSON) {
                            objectRoutine.addStatement("$T $L = gson.toJson($L)",
                                    String.class,
                                    returnSubjectInstanceName + "Json",
                                    returnSubjectInstanceName
                            );
                            objectRoutine.addStatement("$T $L = $S",
                                    String.class,
                                    returnSubjectInstanceName + "ExpectedJson",
                                    serializedValue
                            );
                            returnValue = returnSubjectInstanceName + "ExpectedJson";
                            returnSubjectInstanceName = returnSubjectInstanceName + "Json";
                        }
                    }


                    if (returnType.equals("Ljava/lang/String;") && returnParameter.getName() == null) {
                        objectRoutine.addStatement("$T.assertEquals($L, $L)",
                                assertClass,
                                returnParameter.getValue(),
                                returnSubjectInstanceName
                        );
                    } else {
                        if (returnType.equals("Ljava.lang.Boolean;") || returnType.equals("Z")) {
                            if ((long) returnValue == 1) {
                                returnValue = "true";
                            } else {
                                returnValue = "false";
                            }
                        }
                        objectRoutine.addStatement("$T.assertEquals($L, $L)",
                                assertClass,
                                returnValue,
                                returnSubjectInstanceName
                        );
                    }


                    objectRoutine.addComment("");

                }
            }
        }
    }

    @NotNull
    private String createMethodParametersString(TestCandidateMetadata testCandidateMetadata) throws IOException {
        StringBuilder parameterStringBuilder = new StringBuilder();

        List<Parameter> parameterValues = testCandidateMetadata.getParameterValues();
        for (int i = 0; i < parameterValues.size(); i++) {
            Parameter parameterValue = parameterValues.get(i);

            if (i > 0) {
                parameterStringBuilder.append(", ");
            }

            if (parameterValue.getName() != null) {
                parameterStringBuilder.append(parameterValue.getName());
            } else {
                parameterStringBuilder.append(parameterValue.getValue());
            }


        }


        @NotNull String parameterString = parameterStringBuilder.toString();
        return parameterString;
    }

    @NotNull
    private MethodSpec.Builder buildTestCaseSkeleton(TestCandidateMetadata metadata) {
        MethodSpec.Builder testMethodBuilder =
                MethodSpec.methodBuilder(metadata.getTestMethodName())
                        .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                        .returns(void.class)
                        .addAnnotation(JUNIT_CLASS_NAME)
                        .addStatement("/**")
                        .addStatement("$S", "Testing method: " + metadata.getMethodName())
                        .addStatement("$S", "In class: " + metadata.getFullyQualifiedClassname())
                        .addStatement("$S", "Method has " + metadata.getParameterValues().size() + " " +
                                "parameters.");


        String objectTypeInfo = metadata.getReturnParameter().getType();
        testMethodBuilder.addStatement("method returned a value of type: $S",
                objectTypeInfo + " => " + metadata.getReturnParameter());

        testMethodBuilder.addStatement("todo: add new variable for each parameter");
        testMethodBuilder.addStatement("**/");
        return testMethodBuilder;
    }

    @NotNull
    private MethodSpec.Builder buildJUnitTestCaseSkeleton(String testMethodName) {
        return MethodSpec.methodBuilder(testMethodName)
                .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                .returns(void.class)
                .addAnnotation(JUNIT_CLASS_NAME);
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

        HashMap<Integer, Boolean> doneSignatures = new HashMap<>();
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
                checkProgressIndicator("Generating test case using history of object [ " + i +
                        "/" + total + " ]", null);

                VariableContainer variableContainer = new VariableContainer();

                Parameter objectParameter = new Parameter();
                objectParameter.setValue(objectId);
                ObjectRoutineContainer classTestSuite = generateTestCaseFromObjectHistory(
                        objectParameter, Set.of(), variableContainer);
                int testHash = Arrays.hashCode(classTestSuite.getStatements().toArray());


                // dropping some kind of duplicates
                if (doneSignatures.containsKey(testHash)) {
                    continue;
                }
                doneSignatures.put(testHash, true);

                ObjectRoutine constructorRoutine = classTestSuite.getConstructor();

                for (ObjectRoutine objectRoutine : classTestSuite.getObjectRoutines()) {
                    if (objectRoutine.getRoutineName().equals("<init>")) {
                        continue;
                    }
                    if (objectRoutine.getStatements().size() == 0) {
                        continue;
                    }

                    MethodSpec.Builder builder = MethodSpec.methodBuilder(
                            "testAsInstance" + upperInstanceName(objectRoutine.getRoutineName()));

                    builder.addModifiers(javax.lang.model.element.Modifier.PUBLIC);
                    builder.addException(Exception.class);

                    ObjectRoutineContainer e1 =
                            new ObjectRoutineContainer(List.of(constructorRoutine, objectRoutine));
//                    e1.getObjectRoutines().clear();
//                    e1.getObjectRoutines().add(constructorRoutine);
//                    e1.getObjectRoutines().add(objectRoutine);
                    addRoutinesToMethodBuilder(builder, List.of(e1), new LinkedList<>());


                    builder.addAnnotation(JUNIT_CLASS_NAME);

                    MethodSpec methodTestScript = builder.build();


                    testCaseScripts.add(methodTestScript);

                }


            }

            if (testCaseScripts.size() > MAX_TEST_CASE_LINES) {
                logger.warn("have more than " + MAX_TEST_CASE_LINES + " lines in the script, dropping some");
                testCaseScripts = testCaseScripts.subList(0, MAX_TEST_CASE_LINES);
            }


            checkProgressIndicator(null, "Generate java source for test scenario");
            String generatedTestClassName = "Test" + simpleClassName + "V";
            TypeSpec.Builder typeSpecBuilder = TypeSpec
                    .classBuilder(generatedTestClassName)
                    .addModifiers(
                            javax.lang.model.element.Modifier.PUBLIC,
                            javax.lang.model.element.Modifier.FINAL)
                    .addMethods(testCaseScripts);

            ClassName gsonClass = ClassName.get("com.google.gson", "Gson");

            typeSpecBuilder
                    .addField(FieldSpec
                            .builder(gsonClass,
                                    "gson", javax.lang.model.element.Modifier.PRIVATE)
                            .initializer("new $T()", gsonClass)
                            .build());


            TypeSpec helloWorld = typeSpecBuilder.build();

            JavaFile javaFile = JavaFile.builder(packageName, helloWorld)
                    .build();

            TestCaseUnit testCaseUnit = new TestCaseUnit(
                    javaFile.toString(), packageName, generatedTestClassName);

            testCases.add(testCaseUnit);

        }


        checkProgressIndicator(null, "Generated" + testCases.size() + " test cases");
        return new TestSuite(testCases);


    }

    private void addRoutinesToMethodBuilder(
            MethodSpec.Builder builder,
            List<ObjectRoutineContainer> dependentObjectsList, List<String> variableContainer) {
        for (ObjectRoutineContainer objectRoutineContainer : dependentObjectsList) {

            if (variableContainer.contains(objectRoutineContainer.getName())) {
                // variable has already been initialized
            } else {
                ObjectRoutine constructorRoutine = objectRoutineContainer.getConstructor();
                addRoutinesToMethodBuilder(builder, constructorRoutine.getDependentList(), variableContainer);

                for (Pair<CodeLine, Object[]> statement : constructorRoutine.getStatements()) {
                    CodeLine line = statement.getFirst();
                    if (line instanceof StatementCodeLine) {
                        builder.addStatement(line.getLine(), statement.getSecond());
                    } else {
                        builder.addComment(line.getLine(), statement.getSecond());
                    }
                }
                variableContainer.add(objectRoutineContainer.getName());
            }

            for (ObjectRoutine objectRoutine : objectRoutineContainer.getObjectRoutines()) {
                if (objectRoutine.getRoutineName().equals("<init>")) {
                    continue;
                }

                addRoutinesToMethodBuilder(builder, objectRoutine.getDependentList(), variableContainer);

                for (Pair<CodeLine, Object[]> statement : objectRoutine.getStatements()) {
                    CodeLine line = statement.getFirst();
                    if (line instanceof StatementCodeLine) {
                        builder.addStatement(line.getLine(), statement.getSecond());
                    } else {
                        builder.addComment(line.getLine(), statement.getSecond());
                    }

                }

            }


        }

    }

    /**
     * this is our main man in the team
     *
     * @param parameter
     * @param dependentObjectIdsOriginal
     * @param globalVariableContainer
     * @return
     * @throws APICallException
     * @throws IOException
     */
    private ObjectRoutineContainer
    generateTestCaseFromObjectHistory
    (
            Parameter parameter,
            final Set<Long> dependentObjectIdsOriginal,
            VariableContainer globalVariableContainer
    ) throws APICallException,
            IOException {

        ObjectRoutineContainer objectRoutineContainer = new ObjectRoutineContainer();

        // we want to create the objects from java.lang.* namespace using their real values, so
        // in the test case it looks something like
        // Integer varName = value;
        if (parameter.getType() != null && parameter.getType().startsWith("L" + "java/lang/")) {

            if (globalVariableContainer.contains(parameter.getName())) {
                logger.warn("variable already exists: " + parameter.getName());
                return objectRoutineContainer;
            }


            buildTestCandidateForBaseClass(objectRoutineContainer, parameter);
            return objectRoutineContainer;

        }

        if (parameter.getType() != null && parameter.getType().length() == 1) {

            if (globalVariableContainer.contains(parameter.getName())) {
                logger.warn("variable already exists: " + parameter.getName());
                return objectRoutineContainer;
            }

            buildTestCandidateForBaseClass(objectRoutineContainer,
                    parameter);
            return objectRoutineContainer;


        }

        Set<Long> dependentObjectIds = new HashSet<>(dependentObjectIdsOriginal);


        PageInfo pagination = new PageInfo(0, 500000, PageInfo.Order.ASC);

        FilteredDataEventsRequest request = new FilteredDataEventsRequest();
        request.setPageInfo(pagination);
        request.setObjectId(Long.valueOf(String.valueOf(parameter.getValue())));
        ReplayData objectReplayData = client.
                fetchObjectHistoryByObjectId(request);


        String subjectName = buildTestCandidates(parameter, objectRoutineContainer, objectReplayData);

        if (subjectName != null) {
            objectRoutineContainer.setName(subjectName);
        }

        for (ObjectRoutine objectRoutine : objectRoutineContainer.getObjectRoutines()) {

            if (objectRoutine.getMetadata().size() == 0) {
                continue;
            }

            VariableContainer variableContainer = globalVariableContainer.clone();
            processVariables(objectRoutine.getMetadata(), variableContainer);
            if (objectRoutine.getRoutineName().equals("<init>")) {
                globalVariableContainer = variableContainer;
            }
            objectRoutine.setVariableContainer(variableContainer);
            for (String name : variableContainer.getNames()) {
                Parameter localVariable = variableContainer.getParameterByName(name);
                if (globalVariableContainer.getParameterByName(localVariable.getName()) == null) {
                    Optional<Parameter> byId = globalVariableContainer.getParametersById(localVariable.getType());
                    if (byId.isPresent()) {
                        byId.get().setName(localVariable.getName());
                    }
                }
            }


            Set<Long> newPotentialObjects = new HashSet<>();
            List<Parameter> dependentParameters = new LinkedList<>();


            dependentParameters.addAll(
                    objectRoutine
                            .getMetadata()
                            .stream()
                            .map(TestCandidateMetadata::getParameterValues)
                            .flatMap(Collection::stream)
                            .collect(Collectors.toList()));


            newPotentialObjects.addAll(dependentObjectIds);
            newPotentialObjects.addAll(
                    dependentParameters
                            .stream()
                            .map(e -> e.getProb().getValue())
                            .collect(Collectors.toList()));

            Long parameterValue;
            if (parameter.getValue() instanceof Long) {

                parameterValue = (Long) parameter.getValue();
            } else {
                parameterValue = Long.valueOf(String.valueOf(parameter.getValue()));
            }
            dependentObjectIds.add(parameterValue);
            for (Parameter dependentParameter : dependentParameters) {

                if (dependentParameter.getName() == null) {
                    continue;
                }
                if (variableContainer.contains(dependentParameter.getName())) {
                    logger.warn("variable already exists: " + dependentParameter.getName());
                    continue;
                }

                DataEventWithSessionId parameterProbe = dependentParameter.getProb();
//                if (dependentObjectIds.contains(parameterProbe.getValue())) {
//                    logger.debug("object is already being constructed: " + parameterProbe.getValue());
//                    continue;
//                }

                dependentObjectIds.add(parameterProbe.getValue());

                ObjectRoutineContainer dependentObjectCreation =
                        generateTestCaseFromObjectHistory(
                                dependentParameter,
                                newPotentialObjects, variableContainer);
                variableContainer.add(dependentParameter);
                if (dependentObjectCreation.getName() != null) {
                    dependentParameter.setName(dependentObjectCreation.getName());
                } else {
                    dependentObjectCreation.setName(dependentParameter.getName());
                }

                objectRoutine.addDependent(dependentObjectCreation);
            }
        }

        for (ObjectRoutine objectRoutine : objectRoutineContainer.getObjectRoutines()) {
            if (objectRoutine.getMetadata().size() == 0) {
                continue;
            }
            buildTestFromTestMetadataSet(objectRoutine);
        }


        return objectRoutineContainer;

    }

    /**
     * the method uses the parameter as a filter, and iterates thru all the events in the
     * replayData parameter. Any matching CALL or METHOD_ENTRY is a potential call and its
     * parameters and return value is captured, which serves the bases of the test candidate.
     *
     * @param parameter              the value is the most important part
     * @param objectRoutineContainer the identifies method calles on this parameter will be added
     *                               as a parameter to the object routine container
     * @param objectReplayData       the series of events based on which we are rebuilding the object history
     * @return a name for the target object (which was originally a long id),
     * @throws APICallException this happens when we fail to read the data from the disk or the
     *                          network
     */
    private String buildTestCandidates(
            Parameter parameter,
            ObjectRoutineContainer objectRoutineContainer,
            ReplayData objectReplayData
    ) throws APICallException {


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

        objectReplayData.getObjectInfo().putAll(callContext.getObjectInfo());
        objectReplayData.getTypeInfo().putAll(callContext.getTypeInfo());
        objectReplayData.getStringInfoMap().putAll(callContext.getStringInfoMap());


        List<DataEventWithSessionId> objectEventsReverse =
                new ArrayList<>(objectReplayData.getDataEvents());
        Collections.reverse(objectEventsReverse);
        objectReplayData.setDataEvents(objectEventsReverse);


        // we need to identify a name for this object some way by using a variable name or a
        // paramter name
        String subjectName = null;


        final Map<String, TypeInfo> typeInfoMap = objectReplayData.getTypeInfo();
        final Map<String, DataInfo> probeInfoMap = objectReplayData.getDataInfoMap();
        final Map<String, ClassInfo> classInfoMap = objectReplayData.getClassInfoMap();
        final Map<String, MethodInfo> methodInfoMap = objectReplayData.getMethodInfoMap();
        final Map<String, ObjectInfo> objectInfoMap = objectReplayData.getObjectInfo();

        final ObjectInfo subjectObjectInfo =
                objectInfoMap.get(String.valueOf(parameter.getValue()));
        final TypeInfo subjectTypeInfo =
                typeInfoMap.get(String.valueOf(subjectObjectInfo.getTypeId()));


        long threadId = -1;
        TypeInfo typeInfo;
        for (int eventIndex = 0; eventIndex < objectEvents.size(); eventIndex++) {
            DataEventWithSessionId dataEvent = objectEvents.get(eventIndex);
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
            String constructorOwnerClass;
            constructorOwnerClass = currentClassInfo
                    .getClassName()
                    .split("\\$")[0]
                    .replaceAll("/", ".");

            MethodInfo methodInfo = methodInfoMap.get(String.valueOf(probeInfo.getMethodId()));
            switch (probeInfo.getEventType()) {
                case CALL:

                    constructorOwnerClass = probeInfo.getAttribute("Owner", "").replaceAll("/", ".");

                    if (subjectTypeInfo != null &&
                            !Objects.equals(subjectTypeInfo.getTypeNameFromClass(),
                                    constructorOwnerClass)) {
                        continue;
                    }

                    MethodInfo methodEntryInfo = getMethodInfo(objectEvents.size() - eventIndex - 1, objectReplayData);
                    if (methodEntryInfo != null) {
                        methodInfo = methodEntryInfo;
                    }

                case METHOD_ENTRY:

                    if (StringUtil.isEmpty(constructorOwnerClass)) {
                        continue;
                    }
                    if (subjectTypeInfo != null &&
                            !Objects.equals(
                                    subjectTypeInfo.getTypeNameFromClass(),
                                    constructorOwnerClass)) {
                        continue;
                    }

                    if (methodInfo.getMethodName().equals("<clinit>")) {
                        continue;
                    }


                    TestCandidateMetadata newTestCaseMetadata =
                            TestCandidateMetadata.create(
                                    methodInfo, dataEvent.getNanoTime(), objectReplayData);

                    Parameter testSubjectParameter = newTestCaseMetadata.getTestSubject();
                    if (testSubjectParameter == null) {
                        // whats happening here, if we were unable to pick a test subject
                        // parameter then when dont know which variable to invoke this method on
                        // potentially be a pagination issue also
                        continue;
                    }
                    if (testSubjectParameter.getValue() == null ||
                            !(String.valueOf(testSubjectParameter.getValue()))
                                    .equals(String.valueOf(parameter.getValue()))
                    ) {
                        continue;
                    }


                    // we should find a return parameter even if the type of the return
                    // is void, return parameter marks the completion of the method
                    if (newTestCaseMetadata.getReturnParameter() == null) {
                        logger.debug("skipping method_entry, failed to find call return: " + methodInfo + " -> " + dataEvent);
                        continue;
                    }


                    if (methodInfo.getMethodName().equals("<init>")) {
                        objectRoutineContainer.getConstructor().setMetadata(newTestCaseMetadata);
                    } else {
                        if (subjectName == null) {
                            subjectName = newTestCaseMetadata.getTestSubject().getName();
                        }
                        long currentThreadId = dataEvent.getThreadId();
                        if (currentThreadId != threadId) {
                            // this is happening on a different thread
                            objectRoutineContainer.newRoutine("thread" + currentThreadId);
                            threadId = currentThreadId;
                        }
                        objectRoutineContainer.addMetadata(newTestCaseMetadata);
                    }

                    eventIndex =
                            objectEvents.size() - newTestCaseMetadata.getReturnParameter().getIndex();


                    callStack += 1;
                    break;

            }

        }
        return subjectName;
    }

    private void buildTestCandidateForBaseClass(ObjectRoutineContainer objectRoutineContainer, Parameter parameter) {

        String javaClassName;
        if (parameter.getType().length() > 1) {
            String[] nameParts = parameter.getType().split(";")[0].split("/");
            javaClassName = nameParts[nameParts.length - 1];
        } else {
            javaClassName = parameter.getProbeInfo().getValueDesc().name();
        }

        TestCandidateMetadata testCandidateMetadata = new TestCandidateMetadata();

        ClassName targetClassname = ClassName.get("java.lang", javaClassName);
        testCandidateMetadata.setTestSubject(null);
        Parameter returnStringParam = new Parameter();
        testCandidateMetadata.setReturnParameter(returnStringParam);
        testCandidateMetadata.setFullyQualifiedClassname("java.lang." + javaClassName);
        testCandidateMetadata.setPackageName("java.lang");
        testCandidateMetadata.setTestMethodName("<init>");
        testCandidateMetadata.setUnqualifiedClassname(javaClassName);
        ObjectRoutine constructor = objectRoutineContainer.getConstructor();
        constructor.addStatement("$T $L = $L", targetClassname, parameter.getName(), parameter.getValue());
        constructor.setMetadata(testCandidateMetadata);
    }

    private void processVariables(List<TestCandidateMetadata> metadataList, VariableContainer variableContainer) {
        for (TestCandidateMetadata testCandidateMetadata : metadataList) {

            Parameter testSubject = testCandidateMetadata.getTestSubject();
            if (testCandidateMetadata.getMethodName().equals("<init>")) {

                if (testSubject.getValue() == null) {
                    // this is a constructor call, going directly to as a parameter
                    // something.method(new ClassHere(), ..)
                    // we will create a variable for this first and then use it
                    // we could potentially deal with it directly when generating the actual user
                    // call instead of creating a variable for every anon constructor

                    String variableName =
                            variableContainer.createVariableName(testCandidateMetadata.getUnqualifiedClassname());
                    testSubject.setName(variableName);
                }
                variableContainer.add(testSubject);
                continue;

            }

            if (variableContainer.contains(testSubject.getName())) {

            } else {
                Optional<Parameter> parameterByValue
                        = variableContainer.getParametersById((String) testSubject.getValue());
                if (parameterByValue.isPresent()) {
                    parameterByValue.get().setName(testSubject.getName());
                } else {
                    variableContainer.add(testSubject);
                }
            }


        }
    }

    private MethodInfo getMethodInfo(int eventIndex, ReplayData objectReplayData) {
        int callStack = 0;
        int direction = -1;
        for (int i = eventIndex + direction; i < objectReplayData.getDataEvents().size() && i > -1; i += direction) {
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
