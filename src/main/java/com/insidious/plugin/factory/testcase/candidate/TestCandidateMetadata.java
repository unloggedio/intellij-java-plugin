package com.insidious.plugin.factory.testcase.candidate;

import com.insidious.common.weaver.ClassInfo;
import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.EventType;
import com.insidious.common.weaver.MethodInfo;
import com.insidious.plugin.client.exception.SessionNotSelectedException;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.extension.model.DirectionType;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.extension.model.ScanResult;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.factory.testcase.expression.ExpressionFactory;
import com.insidious.plugin.factory.testcase.TestCaseRequest;
import com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory;
import com.insidious.plugin.factory.testcase.parameter.ParameterFactory;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.expression.Expression;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScript;
import com.insidious.plugin.factory.testcase.writer.TestCaseWriter;
import com.insidious.plugin.pojo.EventMatchListener;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.ScanRequest;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.insidious.plugin.factory.testcase.writer.TestScriptWriter.in;

public class TestCandidateMetadata {
    private final static Logger logger = LoggerUtil.getInstance(TestCandidateMetadata.class);
    private List<MethodCallExpression> methodCallExpressions = new LinkedList<>();
    private VariableContainer fields = new VariableContainer();
    private Expression mainMethod;
    private String fullyQualifiedClassname;
    private String unqualifiedClassname;
    private String packageName;
    private String testMethodName;
    private Parameter testSubject;
    private long callTimeNanoSecond;
    private boolean isArray;

    public static TestCandidateMetadata create(
            List<String> typeHierarchy,
            MethodInfo methodInfo,
            long entryProbeDataId,
            ReplayData replayData,
            TestCaseRequest testCaseRequest) throws APICallException, SessionNotSelectedException {
        logger.warn("[" + methodInfo.getMethodName() + "] " +
                "create test case metadata for types [" + entryProbeDataId + "] -> entry probe " +
                " types  " + typeHierarchy);
        TestCandidateMetadata metadata = new TestCandidateMetadata();


//        final String className = methodInfo.getClassName();
        String targetMethodName = methodInfo.getMethodName();

        if (targetMethodName.startsWith("lambda$")) {
            // this function is a transformation of the original user function by jvm and the
            // name looks like "lambda$ORIGINAL_NAME$<int>"
            targetMethodName = targetMethodName.split("\\$")[1];
        }

        metadata.setFullyQualifiedClassname(typeHierarchy.get(0));

        String[] classNameParts = typeHierarchy.get(0).split("\\.");
        String unqualifiedClassName = classNameParts[classNameParts.length - 1];
        metadata.setUnqualifiedClassname(unqualifiedClassName);
        String packageName = StringUtil.join(classNameParts, ".");
        int lastDotIndex = packageName.lastIndexOf(".");
        if (lastDotIndex > -1) {
            packageName = packageName.substring(0, lastDotIndex);
        } else {
            packageName = "";
        }
        metadata.setPackageName(packageName);


//        Map<String, DataInfo> probeInfoMap = replayData.getProbeInfoMap();


        String testMethodName = "test" + ClassTypeUtils.upperInstanceName(targetMethodName);
        metadata.setTestMethodName(testMethodName);

        // https://gist.github.com/VijayKrishna/6160036
        List<String> methodParameterDescriptions =
                ClassTypeUtils.splitMethodDesc(methodInfo.getMethodDesc());

        String returnParameterDescription = methodParameterDescriptions.remove(methodParameterDescriptions.size() - 1);
        if (methodInfo.getMethodName().equals("<init>")) {
            returnParameterDescription = ClassTypeUtils.getDescriptorName(typeHierarchy.get(0));
        }


        List<DataEventWithSessionId> dataEvents = replayData.getDataEvents();


        int entryProbeIndex = 0;
        // going up matching the first event for our method entry probe
        // up is back in time
        while (entryProbeIndex < dataEvents.size()) {
            DataEventWithSessionId eventInfo = dataEvents.get(entryProbeIndex);
            if (eventInfo.getNanoTime() == entryProbeDataId) break;
            entryProbeIndex++;
        }


        DataEventWithSessionId entryProbe = dataEvents.get(entryProbeIndex);
        DataInfo entryProbeInfo = replayData.getProbeInfo(entryProbe.getDataId());
        logger.info("located entry probe at: " + entryProbeIndex + " -- " + entryProbeInfo);

        int currentEntryProbeIndex = entryProbeIndex;
        ScanResult callReturnScanResult = new ScanResult(currentEntryProbeIndex, 0);
        int pageSize = 1000;
        while (true) {

            if (methodInfo.getMethodName().equals("<init>")) {
                logger.info("entry probe is of type method entry <init>");
                callReturnScanResult = searchMethodExitIndex(replayData,
                        callReturnScanResult,
                        List.of(EventType.METHOD_OBJECT_INITIALIZED));
            } else {
                logger.info("entry probe is of type method entry " + methodInfo.getMethodName());
                callReturnScanResult = searchMethodExitIndex(replayData,
                        callReturnScanResult, List.of(EventType.METHOD_NORMAL_EXIT));
            }


            if (callReturnScanResult.getIndex() != -1) {
                break;
            }
            logger.warn("return probe not found, fetching next page: " +
                    replayData.getFilteredDataEventsRequest().getPageInfo());

            DataEventWithSessionId lastEvent = replayData.getDataEvents().get(0);
            if (Objects.equals(replayData.getFilteredDataEventsRequest().getSortOrder(), "ASC")) {
                lastEvent = replayData.getDataEvents().get(replayData.getDataEvents().size() - 1);
            }
            ReplayData nextPage = replayData.fetchEventsPost(lastEvent, pageSize);
            pageSize = pageSize * 2;
            if (pageSize > 100000) {
                pageSize = 100000;
            }
            if (nextPage.getDataEvents().size() < 2) {
                break;
            }
            callReturnScanResult = new ScanResult(nextPage.getDataEvents().size() - 1,
                    callReturnScanResult.getCallStack());

            replayData.mergeReplayData(nextPage);
        }

        // we are going to do this again, since the index might have shifted from the added pages
        // in the search for return index
        entryProbeIndex = 0;
        // going up matching the first event for our method entry probe
        // up is back in time
        while (entryProbeIndex < dataEvents.size()) {
            DataEventWithSessionId eventInfo = dataEvents.get(entryProbeIndex);
            if (eventInfo.getNanoTime() == entryProbeDataId) break;
            entryProbeIndex++;
        }


//        metadata.setExitProbeIndex(callReturnIndex);
        logger.info("entry probe matched at event: " + entryProbeIndex +
                ", return found " + "at: " + callReturnScanResult.getIndex());

        if (callReturnScanResult.getIndex() == -1) {
            logger.warn("call_return probe not found in the slice: " + entryProbeDataId +
                    " when generating test for method " + targetMethodName + " " +
                    " in class " + typeHierarchy.get(0) + ". Maybe need a bigger " +
                    "slice");
            return metadata;
        }
        long callTime = dataEvents.get(callReturnScanResult.getIndex()).getRecordedAt() - dataEvents.get(entryProbeIndex).getRecordedAt();
        metadata.setCallTimeNanoSecond(callTime);

        VariableContainer methodArguments = searchMethodArguments(replayData, entryProbeIndex, methodParameterDescriptions);

        VariableContainer fieldsContainer = searchMethodFieldsAccessed(replayData, entryProbeIndex);

        VariableContainer variableContainer = new VariableContainer();
        for (Parameter methodParameter : methodArguments.all()) {
            variableContainer.add(methodParameter);
        }


        List<MethodCallExpression> callsList =
                searchMethodCallExpressions(replayData, entryProbeIndex, typeHierarchy,
                        variableContainer, testCaseRequest.getNoMockClassList());

        metadata.setCallList(callsList);
        metadata.addAllFields(fieldsContainer);


//        logger.warn("create return parameter from event at index: " + callReturnIndex);
        Parameter returnParameter = ParameterFactory.createMethodArgumentParameter(callReturnScanResult.getIndex(),
                replayData, 0, returnParameterDescription);
        if (returnParameter.getName() == null || returnParameter.getName().length() == 1) {
            if (methodInfo.getMethodName().equals("<init>")) {
                returnParameter.setName(ClassTypeUtils.createVariableName(methodInfo.getMethodName()));
            }
        }


        String testSubjectInstanceName = ClassTypeUtils.lowerInstanceName(unqualifiedClassName);

//        int paramsToSkip = methodParameters.size();
        int paramsToSkip = 0;
        boolean subjectNameFound = false;


        // identify the variable name on which this method is called
        if (methodInfo.getMethodName().equals("<init>")) {
            metadata.setTestSubject(returnParameter);
            subjectNameFound = true;
        } else {

            int callStack = 0;
            for (int i = entryProbeIndex; i < dataEvents.size(); i += 1) {
                DataEventWithSessionId event = dataEvents.get(i);
                DataInfo probeInfo = replayData.getProbeInfo(event.getDataId());
                ClassInfo currentClassInfo = replayData.getClassInfo(probeInfo.getClassId());
                MethodInfo methodInfoLocal = replayData.getMethodInfo(probeInfo.getMethodId());
                EventType eventType = probeInfo.getEventType();


                switch (eventType) {
                    case CALL_PARAM:
                        if (callStack == -1) {
                            paramsToSkip++;
                        }
                        break;
                    case METHOD_NORMAL_EXIT:
                        callStack++;
                        break;
                    case METHOD_ENTRY:
                        callStack--;
                        break;
                    case LOCAL_LOAD:
                    case GET_STATIC_FIELD:
                    case GET_INSTANCE_FIELD:
                    case NEW_OBJECT_CREATED:
                        if (callStack != -1) {
                            break;
                        }
                        String rawType = ClassTypeUtils.getDottedClassName(
                                probeInfo.getAttribute("Type", "V"));

                        if (!typeHierarchy.contains(rawType)) {
                            paramsToSkip = paramsToSkip - 1;
                            break;
                        }

                        if (paramsToSkip == 0) {
                            String variableName = ClassTypeUtils.getVariableNameFromProbe(probeInfo,
                                    testSubjectInstanceName);

                            Parameter subjectInstanceParameter;
                            if (eventType == EventType.LOCAL_LOAD) {
                                subjectInstanceParameter = ParameterFactory.createParameter(i,
                                        replayData, 0, null);
                            } else {
                                subjectInstanceParameter = ParameterFactory.createParameter(
                                        i - 1, replayData, 0, null);
                            }

                            metadata.setTestSubject(subjectInstanceParameter);
                            LoggerUtil.logEvent("SearchSubject", callStack, i,
                                    event, probeInfo, currentClassInfo, methodInfoLocal);


                            subjectNameFound = true;
                            break;
                        }
                        paramsToSkip = paramsToSkip - 1;

                }

                if (subjectNameFound || callStack < -1) {
                    break;
                }

            }
        }

        MethodCallExpression mainMethodExpression = new MethodCallExpression(
                targetMethodName, metadata.getTestSubject(),
                methodArguments, returnParameter, null
        );

        if (mainMethodExpression.getReturnValue().getName() == null
                && !methodInfo.getMethodName().equals("<init>")) {

            String potentialReturnValueName =
                    ClassTypeUtils.createVariableNameFromMethodName(methodInfo.getMethodName(),
                            metadata.getFullyQualifiedClassname());

            mainMethodExpression.getReturnValue().setName(potentialReturnValueName);
        }


        metadata.setMainMethod(mainMethodExpression);

        return metadata;
    }

    public static MethodCallExpression buildObject(ReplayData replayData,
                                                   final Parameter targetParameter) {
        logger.warn("reconstruct object: " + targetParameter);

        switch (targetParameter.getType()) {
            case "okhttp3.Response":

                AtomicReference<Parameter> responseBodyProbe = new AtomicReference<>();
                AtomicReference<Parameter> responseBodyStringProbe = new AtomicReference<>();

                ScanRequest scanRequest = new ScanRequest(
                        new ScanResult(targetParameter.getIndex(), 0), 0, DirectionType.FORWARDS);

                scanRequest.addListener(EventType.CALL_RETURN, index -> {


                    DataEventWithSessionId callReturnEvent = replayData.getDataEvents().get(index);
                    int callEventIndex = replayData.getPreviousProbeIndex(index);
                    DataEventWithSessionId callEvent = replayData.getDataEvents().get(callEventIndex);
                    DataInfo callEventProbe = replayData.getProbeInfo(callEvent.getDataId());
                    if (callEventProbe.getEventType() != EventType.CALL) {
                        return;
                    }


                    DataInfo callReturnProbeInfo =
                            replayData.getProbeInfo(callReturnEvent.getDataId());

                    @NotNull String returnType = ClassTypeUtils.getDottedClassName(
                            callReturnProbeInfo.getAttribute("Type", "V")
                    );

                    String callOwner = ClassTypeUtils.getDottedClassName(callEventProbe.getAttribute("Owner", ""));
                    String methodName = callEventProbe.getAttribute("Name", null);
                    assert methodName != null;


                    switch (returnType) {
                        case "java.lang.String":
                            if (!methodName.equals("string")) {
                                return;
                            }
                            if (responseBodyProbe.get() == null) {
                                logger.warn("body probe is still missing so this cannot be the " +
                                        "string we are looking for");
                            }
                            Parameter responseBodyProbeInstance = responseBodyProbe.get();
                            if (responseBodyProbeInstance.getProb().getValue() != callEvent.getValue()) {
                                logger.warn("this is not the response body string from the object" +
                                        " we are building: " + callEvent + " -- " + callEventProbe);
                                return;
                            }
                            responseBodyStringProbe.set(ParameterFactory.createMethodArgumentParameter(
                                    index, replayData, 0, "java.lang.String"
                            ));
                            break;
                        case "okhttp3.ResponseBody":
                            if (!methodName.equals("body")) {
                                return;
                            }
                            if (callEvent.getValue() != targetParameter.getProb().getValue()) {
                                logger.warn("this is not the response body from the object we are" +
                                        " building: " + callEvent + " -- " + callEventProbe);
                                return;
                            }
                            if (responseBodyProbe.get() != null) {
                                return;
                            }
                            responseBodyProbe.set(ParameterFactory.createMethodArgumentParameter(
                                    index, replayData, 0, "okhttp3.ResponseBody"
                            ));
                            break;
                    }

                });

                scanRequest.matchUntil(EventType.METHOD_NORMAL_EXIT);
                scanRequest.matchUntil(EventType.METHOD_EXCEPTIONAL_EXIT);

                replayData.eventScan(scanRequest);

                if (responseBodyStringProbe.get() == null) {
                    logger.warn("response body string value not found for okhttp.Response: " + targetParameter);
                }

                if (responseBodyProbe.get() == null) {
                    logger.warn("response body object value not found for okhttp.Response: " + targetParameter);
                }

                VariableContainer variableContainer = VariableContainer.from(
                        List.of(responseBodyStringProbe.get())
                );

                return ExpressionFactory.MethodCallExpression(
                        "buildOkHttpResponseFromString", null, variableContainer,
                        targetParameter, null);
            default:
                break;
        }
        return null;
    }

    private static VariableContainer searchMethodFieldsAccessed(
            ReplayData replayData,
            int entryProbeIndex
    ) {
        VariableContainer variableContainer = new VariableContainer();


        ScanRequest scanRequest = new ScanRequest(new ScanResult(entryProbeIndex, 0),
                ScanRequest.CURRENT_CLASS, DirectionType.FORWARDS);

        scanRequest.addListener(EventType.GET_INSTANCE_FIELD_RESULT, index -> {
            DataEventWithSessionId event = replayData.getDataEvents().get(index);
            DataInfo probeInfo = replayData.getProbeInfo(event.getDataId());

            String fieldType = probeInfo.getAttribute("Type", "V");
            Parameter fieldParameter = ParameterFactory.createParameter(index, replayData, 0, fieldType);
            variableContainer.add(fieldParameter);
        });


        scanRequest.matchUntil(EventType.METHOD_NORMAL_EXIT);
        scanRequest.matchUntil(EventType.METHOD_EXCEPTIONAL_EXIT);

        replayData.eventScan(scanRequest);

        return variableContainer;

    }

    private static List<MethodCallExpression> searchMethodCallExpressions(
            ReplayData replayData,
            int entryProbeIndex,
            List<String> typeHierarchy,
            /*
            variableContainer keeping a track of local variables and method arguments here so
            that we dont record/mock calls on these objects
             */
            VariableContainer variableContainer,
            List<String> noMockClassList) {


        ScanRequest scanRequest = new ScanRequest(new ScanResult(entryProbeIndex, 0),
                ScanRequest.CURRENT_CLASS,
                DirectionType.FORWARDS);

        scanRequest.addListener(EventType.LOCAL_LOAD, index -> {
            Parameter potentialParameter = ParameterFactory.createParameter(
                    index, replayData, 0, null
            );
            variableContainer.add(potentialParameter);
        });

        MethodCallExtractor methodCallExtractor = new MethodCallExtractor(
                replayData, variableContainer, typeHierarchy, noMockClassList
        );
        scanRequest.addListener(EventType.CALL, methodCallExtractor);
        scanRequest.matchUntil(EventType.METHOD_NORMAL_EXIT);
        scanRequest.matchUntil(EventType.METHOD_EXCEPTIONAL_EXIT);

        replayData.eventScan(scanRequest);

        return methodCallExtractor.getCallList();
    }

    private static VariableContainer searchMethodArguments(
            ReplayData replayData,
            int entryProbeIndex,
            List<String> callParameterDescriptions
    ) {
        logger.info("searchCallParameters starting from [" + entryProbeIndex + "] -> " +
                callParameterDescriptions.size() + " params to be found");

        List<Parameter> methodParameterProbes = new LinkedList<>();
        ScanRequest searchRequest = new ScanRequest(new ScanResult(entryProbeIndex, 0), 0,
                DirectionType.FORWARDS);
        searchRequest.matchUntil(EventType.METHOD_NORMAL_EXIT);
        searchRequest.matchUntil(EventType.METHOD_EXCEPTIONAL_EXIT);
        searchRequest.matchUntil(EventType.LABEL);
        searchRequest.matchUntil(EventType.LINE_NUMBER);
        searchRequest.addListener(EventType.METHOD_PARAM, new EventMatchListener() {
            private int paramIndex = 0;

            @Override
            public void eventMatched(Integer callReturnIndex) {
                DataEventWithSessionId event = replayData.getDataEvents().get(callReturnIndex);
                DataInfo probeInfo = replayData.getProbeInfo(event.getDataId());
                ClassInfo currentClassInfo = replayData.getClassInfo(probeInfo.getClassId());
                MethodInfo methodInfoLocal =
                        replayData.getMethodInfoMap().get(String.valueOf(probeInfo.getMethodId()));

                LoggerUtil.logEvent("SearchCallParameters", 0, paramIndex,
                        event, probeInfo, currentClassInfo, methodInfoLocal);

                String expectedParameterType = callParameterDescriptions.get(paramIndex);
                Parameter parameter = ParameterFactory.createMethodArgumentParameter(
                        callReturnIndex, replayData, callParameterDescriptions.size() - paramIndex - 1,
                        expectedParameterType);
                methodParameterProbes.add(parameter);
                paramIndex++;
            }
        });

        replayData.eventScan(searchRequest);
        logger.info("Found [" + methodParameterProbes.size() + "] parameters");

        return VariableContainer.from(methodParameterProbes);
    }

    private static ScanResult searchMethodExitIndex(
            ReplayData replayData,
            ScanResult entryProbeIndex,
            List<EventType> eventTypeMatch
    ) {
        logger.info("looking for call return index, with entry probe index at: " + entryProbeIndex + " -> " + eventTypeMatch);
        logger.info("replay data has: " + replayData.getDataEvents().size() + " events.");


        ScanRequest searchRequest = new ScanRequest(entryProbeIndex, 0,
                DirectionType.FORWARDS);
        for (EventType typeMatch : eventTypeMatch) {
            searchRequest.matchUntil(typeMatch);
        }

        return replayData.eventScan(searchRequest);
    }

    public Expression getMainMethod() {
        return mainMethod;
    }

    public void setMainMethod(Expression mainMethod) {
        this.mainMethod = mainMethod;
    }

    public VariableContainer getFields() {
        return fields;
    }

    public void setFields(VariableContainer fields) {
        this.fields = fields;
    }

    public boolean isArray() {
        return isArray;
    }

    public void setArray(boolean array) {
        isArray = array;
    }

    private void addAllFields(VariableContainer fieldsContainer) {
        fieldsContainer.all().forEach(this.fields::add);
    }

    private void setCallList(List<MethodCallExpression> callsList) {
        this.methodCallExpressions = callsList;
    }

    public String getFullyQualifiedClassname() {
        return fullyQualifiedClassname;
    }

    public void setFullyQualifiedClassname(String fullyQualifiedClassname) {
        this.fullyQualifiedClassname = fullyQualifiedClassname;
    }

    public String getUnqualifiedClassname() {
        return unqualifiedClassname;
    }

    public void setUnqualifiedClassname(String unqualifiedClassname) {
        this.unqualifiedClassname = unqualifiedClassname;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getTestMethodName() {
        return testMethodName;
    }

    public void setTestMethodName(String testMethodName) {
        this.testMethodName = testMethodName;
    }

    public Parameter getTestSubject() {
        return testSubject;
    }

    public void setTestSubject(Parameter testSubject) {
        this.testSubject = testSubject;
        if (this.mainMethod != null && this.mainMethod instanceof MethodCallExpression) {
            MethodCallExpression mce = (MethodCallExpression) this.mainMethod;
            mce.setSubject(testSubject);
        }
    }


    public long getCallTimeNanoSecond() {
        return callTimeNanoSecond;
    }

    public void setCallTimeNanoSecond(long callTimeNanoSecond) {
        this.callTimeNanoSecond = callTimeNanoSecond;
    }

    public List<MethodCallExpression> getCallsList() {
        return methodCallExpressions;
    }

    @Override
    public String toString() {
        return "TestCandidateMetadata{" +
                ", fullyQualifiedClassname='" + fullyQualifiedClassname + '\'' +
                ", testMethodName='" + testMethodName + '\'' +
                ", testSubject=" + testSubject +
                ", callTimeNanoSecond=" + callTimeNanoSecond +
                '}';
    }

    public void setIsArray(boolean isArray) {
        this.isArray = isArray;
    }

    public ObjectRoutineScript toObjectScript() {
        ObjectRoutineScript objectRoutineScript = new ObjectRoutineScript();

        if (getMainMethod() instanceof MethodCallExpression) {


            MethodCallExpression mainMethod = (MethodCallExpression) getMainMethod();

            objectRoutineScript.addComment("");
            Parameter mainMethodReturnValue = mainMethod.getReturnValue();


            DataEventWithSessionId mainMethodReturnValueProbe = mainMethodReturnValue.getProb();
            if (mainMethodReturnValueProbe == null) {
                return objectRoutineScript;
            }
            objectRoutineScript.addComment("Test candidate method [" + mainMethod.getMethodName() + "] " +
                    "[ " + mainMethodReturnValueProbe.getNanoTime() + "] - took " +
                    Long.valueOf(getCallTimeNanoSecond() / (1000000)).intValue() + "ms");

            VariableContainer arguments = mainMethod.getArguments();
            if (arguments.count() > 0) {


                objectRoutineScript.addComment("");
                for (Parameter parameter : arguments.all()) {
                    if (parameter.getName() == null && parameter.getProb().getSerializedValue().length > 0) {
                        String serializedValue = new String(parameter.getProb().getSerializedValue());
                        if (parameter.getType().equals("java.lang.String")) {
                            serializedValue = '"' + serializedValue + '"';
                        }
                        parameter.setValue(serializedValue);
                    }
                    objectRoutineScript.addParameterComment(parameter);
                }
                objectRoutineScript.addComment("");
                objectRoutineScript.addComment("");

                for (Parameter parameter : arguments.all()) {
//                    if (!objectRoutine.getCreatedVariables().contains(parameter.getName())) {
                    in(objectRoutineScript).assignVariable(parameter).fromRecordedValue().endStatement();
//                    }
                }

            }


            Map<String, MethodCallExpression> mockedCalls = new HashMap<>();
            if (getCallsList().size() > 0) {

                objectRoutineScript.addComment("");
                for (MethodCallExpression methodCallExpression : getCallsList()) {
                    if (methodCallExpression.getException() != null &&
                            mockedCalls.containsKey(methodCallExpression.getMethodName())) {
                        continue;
                    }
                    TestCaseWriter.createMethodCallComment(objectRoutineScript, methodCallExpression);
                    TestCaseWriter.createMethodCallMock(objectRoutineScript, methodCallExpression);
                    mockedCalls.put(methodCallExpression.getMethodName(), methodCallExpression);
                }
                objectRoutineScript.addComment("");
                objectRoutineScript.addComment("");
            }
//
            Expression testCandidateMainMethod = getMainMethod();
            if (testCandidateMainMethod instanceof MethodCallExpression) {
                MethodCallExpression mainMethodExpression = (MethodCallExpression) testCandidateMainMethod;
                for (Parameter parameter : mainMethodExpression.getArguments().all()) {
//                objectRoutine.getCreatedVariables().add(parameter);
                    if (!objectRoutineScript.getCreatedVariables().contains(parameter.getName())) {
                        in(objectRoutineScript).assignVariable(parameter).fromRecordedValue().endStatement();
                    }
                }
            }
//

            //////////////////////// FUNCTION CALL ////////////////////////

            // return type == V ==> void return type => no return value
            in(objectRoutineScript).assignVariable(mainMethodReturnValue).writeExpression(mainMethod).endStatement();


            if (mainMethod.getMethodName().equals("<init>")) {
                // there is no verification required (?) after calling constructors
                return objectRoutineScript;
            }
            String returnSubjectInstanceName = mainMethodReturnValue.getName();


            //////////////////////////////////////////////// VERIFICATION ////////////////////////////////////////////////


            // deserialize and compare objects
            byte[] serializedBytes = mainMethodReturnValueProbe.getSerializedValue();


            Parameter returnSubjectExpectedJsonString = null;
            if (serializedBytes.length > 0) {
                returnSubjectExpectedJsonString = ParameterFactory.createStringByName(returnSubjectInstanceName + "ExpectedJson");
                in(objectRoutineScript)
                        .assignVariable(returnSubjectExpectedJsonString)
                        .writeExpression(ExpressionFactory.StringExpression(new String(serializedBytes)))
                        .endStatement();
            } else {
                returnSubjectExpectedJsonString = ParameterFactory.createStringByName(returnSubjectInstanceName + "ExpectedJson");

                in(objectRoutineScript)
                        .assignVariable(returnSubjectExpectedJsonString)
                        .writeExpression(ExpressionFactory.StringExpression(new String(serializedBytes)))
                        .endStatement();
            }


            // reconstruct object from the serialized form to an object instance in the
            // test method to compare it with the new object, or do it the other way
            // round ? Maybe serializing the object and then comparing the serialized
            // string forms would be more readable ? string comparison would fail if the
            // serialization has fields serialized in random order
            Parameter returnSubjectJsonString = ParameterFactory.createStringByName(returnSubjectInstanceName + "Json");
            in(objectRoutineScript)
                    .assignVariable(returnSubjectJsonString)
                    .writeExpression(MethodCallExpressionFactory.ToJson(mainMethodReturnValue))
                    .endStatement();


            in(objectRoutineScript)
                    .writeExpression(MethodCallExpressionFactory
                            .MockitoAssert(returnSubjectJsonString, returnSubjectExpectedJsonString))
                    .endStatement();

        } else {
            Expression plainValueExpression = getMainMethod();
            logger.warn("PVE: " + plainValueExpression);
//            in(objectRoutine)
//                    .assignVariable()
//                    .writeExpression(MethodCallExpressionFactory
//                            .MockitoAssert(returnSubjectJsonString, returnSubjectExpectedJsonString))
//                    .endStatement();
        }
        objectRoutineScript.addComment("");
        return objectRoutineScript;

    }
}
