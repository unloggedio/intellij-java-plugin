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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

        for (TracePoint tracePoint : tracePointList) {

            DataInfo probeInfo = classWeaveInfo.getProbeById(tracePoint.getDataId());
            MethodInfo methodInfo = classWeaveInfo.getMethodInfoById(probeInfo.getMethodId());
            ClassInfo classInfo = classWeaveInfo.getClassInfoById(methodInfo.getClassId());

            FilteredDataEventsRequest filterDataEventsRequest = tracePoint.toFilterDataEventRequest();

            ReplayData probeReplayData = client.fetchDataEvents(filterDataEventsRequest);

            TestCandidate testCandidate = new TestCandidate(methodInfo, classInfo, session, probeInfo,
                    probeReplayData, classWeaveInfo);
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

        List<TestCaseScript> testCases = new LinkedList<>();


        Map<String, List<TestCandidate>> testCandidatesByClass = testCandidateList.stream()
                .collect(Collectors.groupingBy(e -> e.getClassInfo().getClassName()));

        for (String className : testCandidatesByClass.keySet()) {

            List<TestCandidate> candidates = testCandidatesByClass.get(className);
            logger.info("generate test cases for class: "
                    + className + ": we have " + candidates.size() + " candidates");

            ClassInfo classInfo = candidates.get(0).getClassInfo();
            String fullyQualifiedClassName = classInfo.getClassName();

            String[] classNameParts = fullyQualifiedClassName.split("/");
            String unqualifiedClassName = classNameParts[classNameParts.length - 1];
            String packageName = StringUtil.join(classNameParts, ".");
            int lastDotIndex = packageName.lastIndexOf(".");
            if (lastDotIndex > -1) {
                packageName = packageName.substring(0, lastDotIndex);
            } else {
                packageName = "";
            }

            List<MethodSpec> testMethods = new LinkedList<>();
            for (TestCandidate testCandidate : candidates) {

                MethodInfo methodInfo = testCandidate.getMethodInfo();
                ClassWeaveInfo classWeaveInfo = testCandidate.getClassWeaveInfo();
                Map<String, TypeInfo> typeInfo = testCandidate.getProbeReplayData().getTypeInfo();
                Map<String, ObjectInfo> objectInfoMap = testCandidate.getProbeReplayData().getObjectInfo();
                Map<String, StringInfo> stringInfoMap = testCandidate.getProbeReplayData().getStringInfoMap();

                String methodName = methodInfo.getMethodName();

                if (methodName.equals("<init>")) {
                    logger.warn("skipping <init> method, " +
                            "is a constructor for or in class: " + classInfo.getClassName());
                    continue;
                }

                String potentialClassVariableInstanceName = lowerInstanceName(unqualifiedClassName);

                String testMethodName = "test" + upperInstanceName(methodName);

                // https://gist.github.com/VijayKrishna/6160036
                List<String> methodDescription = splitMethodDesc(methodInfo.getMethodDesc());
                String methodReturnValueType = methodDescription.get(methodDescription.size() - 1);
                methodDescription.remove(methodDescription.size() - 1);


                MethodSpec.Builder testMethodBuilder = MethodSpec.methodBuilder(testMethodName)
                        .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
                        .returns(void.class)
                        .addAnnotation(ClassName.get("org.junit", "Test"))
//                .addParameter(String[].class, "args")
                        .addStatement("/**")
                        .addStatement("$S", "Testing method: " + methodInfo.getMethodName())
                        .addStatement("$S", "In class: " + classInfo.getClassName())
                        .addStatement("$S", "Method has " + methodDescription.size() + " " +
                                "parameters.");


                DataInfo entryProbeInfo = testCandidate.getMethodEntryProbe();
                String probeAttributes = entryProbeInfo.getAttributes();

                List<DataEventWithSessionId> events = testCandidate.getProbeReplayData().getDataEvents();

                int entryProbeIndex = 0;
                List<DataEventWithSessionId> methodParameterProbes = new LinkedList<>();
                boolean lookingForParams = true;
                // going up matching the first event for our method entry probe
                // up is back in time
                while (entryProbeIndex < events.size()) {
                    DataEventWithSessionId eventInfo = events.get(entryProbeIndex);
                    if (eventInfo.getDataId() == entryProbeInfo.getDataId()) break;
                    entryProbeIndex++;
                }

                int direction = -1;
                int callReturnIndex = entryProbeIndex + direction;
                DataEventWithSessionId entryPointEvent = events.get(entryProbeIndex);

                int callStack = 0;
                // going down from where we found the method entry probe
                // to match the first call_return probe
                while (callReturnIndex > -1) {
                    DataEventWithSessionId event = events.get(callReturnIndex);
                    DataInfo eventProbeInfo = classWeaveInfo.getProbeById(event.getDataId());
                    EventType eventType = eventProbeInfo.getEventType();

                    if (eventType == EventType.METHOD_ENTRY) {
                        callStack++;
                    }
                    if (callStack > 0 && eventType == EventType.CALL_RETURN) {
                        callStack--;
                    }
                    if (callStack > 0) {
                        callReturnIndex += direction;
                        continue;
                    }

                    if (lookingForParams && eventType == EventType.METHOD_PARAM) {
                        methodParameterProbes.add(event);
                    } else {
                        lookingForParams = false;
                    }

                    if (eventProbeInfo.getEventType() == EventType.CALL_RETURN) {
                        break;
                    }

                    callReturnIndex += direction;

                }

                logger.info("entry probe matched at event: " + entryProbeIndex + ", return found " +
                        "at: " + callReturnIndex);


                int i = 0;
                List<String> methodParameterValues = new LinkedList<>();
                List<String> methodParameterTypes = new LinkedList<>();

                for (DataEventWithSessionId parameterProbe : methodParameterProbes) {
                    i++;
                    long probeValue = parameterProbe.getValue();
                    String probeValueString = String.valueOf(probeValue);
                    ObjectInfo objectInfo = objectInfoMap.get(probeValueString);
                    assert objectInfo != null;
                    TypeInfo probeType = typeInfo.get(String.valueOf(objectInfo.getTypeId()));
                    testMethodBuilder.addStatement("$S",
                            "Parameter " + i + ": [" + probeType.getTypeNameFromClass() + "] " +
                                    "potential value: [" + probeValueString + "] ");
                    if (stringInfoMap.containsKey(probeValueString)) {
                        String quotedStringValue =
                                "\"" + StringUtil.escapeQuotes(stringInfoMap.get(probeValueString).getContent()) + "\"";
                        methodParameterValues.add(quotedStringValue);
                    } else {
                        methodParameterValues.add(probeValueString);
                    }
                }


                if (callReturnIndex == -1) {
                    logger.warn("call_return probe not found in the slice: " + entryProbeInfo +
                            " when generating test for method " + methodInfo.getMethodName() + " " +
                            " in class " + classInfo.getClassName() + ". Maybe need a bigger " +
                            "slice");
                    continue;
                }

                logger.info("call_return probe found at " + callReturnIndex);
                DataEventWithSessionId callReturnProbe = events.get(callReturnIndex);
                logger.info("call return value is: " + callReturnProbe.getValue());


                String returnProbeValue = String.valueOf(callReturnProbe.getValue());
                ObjectInfo objectInfo = objectInfoMap.get(returnProbeValue);

                StringInfo possibleReturnStringValue = null;
                if (stringInfoMap.containsKey(returnProbeValue)) {
                    possibleReturnStringValue = stringInfoMap.get(returnProbeValue);
                    testMethodBuilder.addStatement("method returned a string: $S", possibleReturnStringValue);
                } else if (objectInfo != null) {
                    TypeInfo objectTypeInfo = typeInfo.get(String.valueOf(objectInfo.getTypeId()));
                    testMethodBuilder.addStatement("method returned a value of type: $S"
                            , objectTypeInfo);
                } else {
                    testMethodBuilder.addStatement("method returned: $S", returnProbeValue);
                }

                testMethodBuilder.addStatement("**/");

                ClassName squareClassName = ClassName.get(packageName, unqualifiedClassName);

                testMethodBuilder.addStatement("$T $L = new $T()", squareClassName,
                        potentialClassVariableInstanceName, squareClassName);

                @NotNull String parameterString = StringUtil.join(methodParameterValues, ", ");

                // return type == V ==> void return type => no return value
                if (methodReturnValueType.equals("V")) {
                    testMethodBuilder.addStatement("$L.$L(" + parameterString + ")",
                            potentialClassVariableInstanceName, methodName);
                } else {


                    String potentialReturnValueName = methodName;
                    if (methodName.startsWith("get") || methodName.startsWith("set")) {
                        if (methodName.length() > 3) {
                            potentialReturnValueName =
                                    lowerInstanceName(methodName.substring(3)) + "Value";
                        } else {
                            potentialReturnValueName = "value";
                        }
                    } else {
                        potentialReturnValueName = methodName + "Result";
                    }


                    Object returnValueSquareClass = null;
                    if (methodReturnValueType.startsWith("L") || methodReturnValueType.startsWith("[")) {
                        returnValueSquareClass = constructClassName(methodReturnValueType);
                    } else {
                        returnValueSquareClass = getClassFromDescriptor(methodReturnValueType);
                    }

                    testMethodBuilder.addStatement("$T $L = $L.$L(" + parameterString + ")",
                            returnValueSquareClass, potentialReturnValueName,
                            potentialClassVariableInstanceName, methodName);

                    if (possibleReturnStringValue == null) {
                        testMethodBuilder.addStatement("assert $L == $S", potentialReturnValueName, returnProbeValue);
                    } else {
                        testMethodBuilder.addStatement("assert $L == $S", potentialReturnValueName, possibleReturnStringValue);
                    }

                    testMethodBuilder.addStatement("// thats all folks");

                }

                testMethods.add(testMethodBuilder.build());


            }


            TypeSpec helloWorld = TypeSpec.classBuilder("Test" + unqualifiedClassName)
                    .addModifiers(javax.lang.model.element.Modifier.PUBLIC,
                            javax.lang.model.element.Modifier.FINAL)
                    .addMethods(testMethods)
                    .build();

            JavaFile javaFile = JavaFile.builder(packageName, helloWorld)
                    .build();

            testCases.add(new TestCaseScript(
                    javaFile.toString(),
                    classInfo
            ));

        }


        return new TestSuite(testCases);

//        BufferedOutputStream boss = new BufferedOutputStream(outputStream);
//        DataOutputStream doss = new DataOutputStream(boss);
//        doss.write("package com.package.test\n".getBytes());
//        doss.write("\t@Test".getBytes());
//        doss.write("\t\tpublic void testMethod1() {\n".getBytes());
//        doss.write("\t}\n".getBytes());


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


    public static List<String> splitMethodDesc(String desc) {
        int beginIndex = desc.indexOf('(');
        int endIndex = desc.lastIndexOf(')');
        if ((beginIndex == -1 && endIndex != -1) || (beginIndex != -1 && endIndex == -1)) {
            System.err.println(beginIndex);
            System.err.println(endIndex);
            throw new RuntimeException();
        }
        String x0;
        if (beginIndex == -1 && endIndex == -1) {
            x0 = desc;
        } else {
            x0 = desc.substring(beginIndex + 1, endIndex);
        }
        Pattern pattern = Pattern.compile("\\[*L[^;]+;|\\[[ZBCSIFDJ]|[ZBCSIFDJ]"); //Regex for desc \[*L[^;]+;|\[[ZBCSIFDJ]|[ZBCSIFDJ]
        Matcher matcher = pattern.matcher(x0);
        List<String> listMatches = new LinkedList<>();
        while (matcher.find()) {
            listMatches.add(matcher.group());
        }
        listMatches.add(desc.substring(endIndex + 1));
        return listMatches;
    }
}
