package com.insidious.plugin.factory.testcase.candidate;

import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.EventType;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.extension.model.DirectionType;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.extension.model.ScanResult;
import com.insidious.plugin.factory.testcase.TestGenerationState;
import com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory;
import com.insidious.plugin.factory.testcase.parameter.ParameterFactory;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScript;
import com.insidious.plugin.pojo.EventMatchListener;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.ScanRequest;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CandidateMetadataFactory {
    public final static Logger logger = LoggerUtil.getInstance(TestCandidateMetadata.class);

    public static ObjectRoutineScript toObjectScript(
            TestCandidateMetadata testCandidateMetadata,
            TestGenerationState testGenerationState,
            TestCaseGenerationConfiguration testConfiguration
    ) {
        ObjectRoutineScript objectRoutineScript = new ObjectRoutineScript(testGenerationState.getVariableContainer(),
                testConfiguration);

        Parameter testTarget = testConfiguration.getTestCandidateMetadataList()
                .get(0)
                .getTestSubject();

        if (testCandidateMetadata.getMainMethod() instanceof MethodCallExpression) {

            MethodCallExpression mainMethod = (MethodCallExpression) testCandidateMetadata.getMainMethod();


            Collection<MethodCallExpression> callToMock = new ArrayList<>();
            Set<MethodCallExpression> staticCallsList = new HashSet<>();

            Map<String, Boolean> mockedStaticTypes = new HashMap<>();

            for (MethodCallExpression e : testCandidateMetadata.getCallsList()) {
                if (!testConfiguration.getCallExpressionList()
                        .contains(e)) {
                    logger.warn("Skip unselected call expression to be mocked - " + e);
                    continue;
                }
                if (e.getReturnValue() == null) {
                    logger.info("MCE to mock without a return value - " + e);
                    continue;
                }
                if (e.isStaticCall() && e.getUsesFields()) {
                    // all static calls need to be mocked
                    // even if they have no return value
                    if (e.getSubject()
                            .getType()
                            .equals(testTarget.getType())) {
                        // we do not want to mock static calls on the class itself being tested
                        // since we most likely have injected all the fields anyways
                        continue;
                    }
                    staticCallsList.add(e);
                    mockedStaticTypes.put(e.getSubject()
                            .getType(), true);
                    continue;
                }
                if (e.getSubject()
                        .getType()
                        .startsWith("com.google.")) {
                    continue;
                }
                if (!e.isMethodPublic() && !e.isMethodProtected()) {
                    continue;
                }
//                DataInfo entryProbeInfo = e.getEntryProbeInfo();
//                if ("INVOKEVIRTUAL".equals(entryProbeInfo.getAttribute("Instruction", ""))
//                        && testCandidateMetadata.getTestSubject().getType().equals(e.getSubject().getType())
//                ) {
//                    // a invokevirtual call, is going to one of its super class,
//                    // and specifically in case of Classes which are children of AbstractDao of hibernate package
//                    // we need to mock the call and also the return object
//                    callToMock.add(e);
//                    // add the return object of this call as a field,
//                    // because we need to mock the calls on the return object as well
//                    testCandidateMetadata.getFields().add(e.getReturnValue());
//                    continue;
//                }
                if (e.getMethodName()
                        .startsWith("<")) {
                    // constructors need not be mocked
                    continue;
                }

                if (e.getSubject() == null) {
                    // not a static call, but we failed to identify subject
                    // this is potentially a bug, and the fix is inside scan implementation
                    continue;
                }
                if (e.getReturnValue()
                        .getType() == null
                        || e.getReturnValue()
                        .getType()
                        .equals("V")
                        || e.getReturnValue()
                        .getProb() == null) {
                    // either the function has no return value (need not be mocked) or
                    // we failed to identify the return value in the scan, in that case this is a bug
                    continue;
                }
                if (e.getSubject()
                        .getType()
                        .contains("com.google")) {
                    // this is hard coded to skip mocking Gson class
                    continue;
                }

                if (testCandidateMetadata.getFields()
                        .getParametersById(e.getSubject()
                                .getProb()
                                .getValue()) == null) {
                    // the subject should ideally be one of the already identified fields.
                    continue;
                }

                // finally add this call in the list of calls that will be actually mocked
                callToMock.add(e);

            }

            // this makes the test case worse instead of better, as a lot of static calls might end up in the test
            // case which have these variables in parameters which are not final but need to be final because we have
            // to match the call based on a syntax like methoCall(eq(value1), any(Class.class))
//            for (MethodCallExpression methodCallExpression : testCandidateMetadata.getCallsList()) {
//
//                if (methodCallExpression.isStaticCall() && mockedStaticTypes.containsKey(
//                        methodCallExpression.getSubject()
//                                .getType())) {
//                    staticCallsList.add(methodCallExpression);
//                }
//
//            }


            Map<String, Boolean> mockedCalls = testGenerationState.getMockedCallsMap();
            if (callToMock.size() > 0) {

                for (MethodCallExpression methodCallExpression : callToMock) {

                    String mockedCallSignature = buildCallSignature(methodCallExpression);


                    if (methodCallExpression.getException() != null || mockedCalls.containsKey(mockedCallSignature)) {
                        continue;
                    }
                    Parameter returnValue = methodCallExpression.getReturnValue();
                    returnValue.getNameForUse(
                            methodCallExpression.getMethodName());

                    if (returnValue.isPrimitiveType()) {
                        returnValue.setTypeForced(ClassTypeUtils.getDottedClassName(returnValue.getProbeInfo()
                                .getAttribute("Type", returnValue.getType())));
                    }

                    methodCallExpression.writeCommentTo(objectRoutineScript);
                    methodCallExpression.writeMockTo(objectRoutineScript, testConfiguration, testGenerationState);
                    mockedCalls.put(mockedCallSignature, true);
                    objectRoutineScript.addComment("");
                }
//                objectRoutineScript.addComment("");
            }


            if (staticCallsList.size() > 0) {

                for (MethodCallExpression methodCallExpression : staticCallsList) {

                    // we have a call SQSUseCase.values(0 args), which returns an array, at callStack 10 when the main callStack is at 4
                    // so the expected call stack should be 5, but we cannot check that since the callStack value with 6 is a valid call to be mocked
                    // but this call which return an array should not be mocked
                    // probably need to exclude calls from inside of <clinit>
                    Parameter returnValue = methodCallExpression.getReturnValue();
//                    if (returnValue.getName() == null) {
//                        continue;
//                    }


                    Parameter staticCallSubjectMockInstance = methodCallExpression.getSubject();

                    String mockedCallSignature = buildCallSignature(methodCallExpression);

                    if (!mockedCalls.containsKey(mockedCallSignature)) {
                        if (!objectRoutineScript.getCreatedVariables()
                                .contains(staticCallSubjectMockInstance.getName())) {
                            @NotNull Parameter subjectStaticFieldMock = Parameter.cloneParameter(
                                    staticCallSubjectMockInstance);

                            subjectStaticFieldMock.setContainer(true);
                            Parameter childParameter = new Parameter();
                            childParameter.setType(staticCallSubjectMockInstance.getType());
                            subjectStaticFieldMock.setType("org.mockito.MockedStatic");
                            subjectStaticFieldMock.getTemplateMap()
                                    .put("E", childParameter);

                            objectRoutineScript.addStaticMock(subjectStaticFieldMock);
                        }

                        mockedCalls.put(mockedCallSignature, true);
                        objectRoutineScript.addComment("Add mock for static method call: " + methodCallExpression);


                        returnValue.getNameForUse(
                                methodCallExpression.getMethodName());

                        if (returnValue.isPrimitiveType()) {
                            returnValue.setTypeForced(ClassTypeUtils.getDottedClassName(returnValue.getProbeInfo()
                                    .getAttribute("Type", returnValue.getType())));
                        }


                        if (!returnValue.getType()
                                .equals("V")) {
                            methodCallExpression.writeMockTo(objectRoutineScript, testConfiguration,
                                    testGenerationState);
                        }

                    }


                }


            }

            if (mainMethod.getMethodName()
                    .equals("<init>")) {
                objectRoutineScript.getCreatedVariables()
                        .add(mainMethod.getSubject());
                mainMethod.setReturnValue(mainMethod.getSubject());
            }

            if (mainMethod.isMethodPublic() && mainMethod.getReturnValue()
                    .getType() != null) {
                mainMethod.writeTo(objectRoutineScript, testConfiguration, testGenerationState);
            }


        } else {
            testCandidateMetadata.getMainMethod()
                    .writeTo(objectRoutineScript, testConfiguration, testGenerationState);
        }
//        objectRoutineScript.addComment("");
        return objectRoutineScript;

    }

    @NotNull
    private static String buildCallSignature(MethodCallExpression methodCallExpression) {
        Parameter subject = methodCallExpression.getSubject();

        StringBuilder callBuilder = new StringBuilder();
        callBuilder.append(subject.getName())
                .append(".")
                .append(methodCallExpression.getMethodName());

        for (Parameter parameter : methodCallExpression.getArguments()) {
//            byte[] serializedValue = parameter.getProb()
//                    .getSerializedValue();
            callBuilder.append(parameter.getValue());
        }
        return callBuilder.toString();
    }

    public static MethodCallExpression buildObject(ReplayData replayData, final Parameter targetParameter) {
        logger.warn("reconstruct object: " + targetParameter);

        switch (targetParameter.getType()) {
            case "okhttp3.Response":

                AtomicReference<Parameter> responseBodyProbe = new AtomicReference<>();
                AtomicReference<Parameter> responseBodyStringProbe = new AtomicReference<>();

                ScanRequest scanRequest = new ScanRequest(
                        new ScanResult(targetParameter.getIndex(), 0, false), 0, DirectionType.FORWARDS);

                scanRequest.addListener(EventType.CALL_RETURN, (index, matchedStack) -> {


                    DataEventWithSessionId callReturnEvent = replayData.getDataEvents()
                            .get(index);
                    int callEventIndex = replayData.getPreviousProbeIndex(index);
                    DataEventWithSessionId callEvent = replayData.getDataEvents()
                            .get(callEventIndex);
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
                            if (responseBodyProbeInstance.getProb()
                                    .getValue() != callEvent.getValue()) {
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
                            if (callEvent.getValue() != targetParameter.getProb()
                                    .getValue()) {
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

                replayData.ScanForEvents(scanRequest);

                if (responseBodyStringProbe.get() == null) {
                    logger.warn("response body string value not found for okhttp.Response: " + targetParameter);
                }

                if (responseBodyProbe.get() == null) {
                    logger.warn("response body object value not found for okhttp.Response: " + targetParameter);
                }

                VariableContainer variableContainer = VariableContainer.from(
                        List.of(responseBodyStringProbe.get())
                );

                MethodCallExpression buildOkHttpResponseFromString =
                        MethodCallExpressionFactory.MethodCallExpression("buildOkHttpResponseFromString",
                                null, variableContainer, targetParameter);
                return buildOkHttpResponseFromString;
            case "java.util.List":


                ScanRequest identifyIteratorScanRequest = new ScanRequest(
                        new ScanResult(targetParameter.getIndex(), 0, false), ScanRequest.CURRENT_CLASS,
                        DirectionType.FORWARDS
                );

                AtomicInteger iteratorProbe = new AtomicInteger(-1);
                AtomicInteger nextValueProbe = new AtomicInteger(-1);
                AtomicReference<Parameter> containedParameterIterator = new AtomicReference<>();
                AtomicReference<Parameter> nextValueParameter = new AtomicReference<>();
                identifyIteratorScanRequest.addListener(targetParameter.getProb()
                                .getValue(),

                        new EventMatchListener() {
                            @Override
                            public void eventMatched(Integer index, int matchedStack) {
                                DataEventWithSessionId event = replayData.getDataEvents()
                                        .get(index);
                                DataInfo probeInfo = replayData.getProbeInfo(
                                        event.getDataId()
                                );
                                if (probeInfo.getEventType() == EventType.CALL) {
                                    String methodName = probeInfo.getAttribute("Name", null);
                                    Parameter containedParameter;
                                    DataInfo callReturnProbeInfo;

                                    switch (methodName) {
                                        case "iterator":
                                            if (iteratorProbe.get() != -1) {
                                                return;
                                            }
                                            assert methodName != null;


                                            int nextProbeIndex = replayData.getNextProbeIndex(index);
                                            DataEventWithSessionId callReturnProbe = replayData
                                                    .getDataEvents()
                                                    .get(nextProbeIndex);
                                            callReturnProbeInfo = replayData.getProbeInfo(callReturnProbe.getDataId());
                                            assert callReturnProbeInfo.getEventType() == EventType.CALL_RETURN;
                                            nextValueProbe.set(index);

                                            Parameter returnParam = ParameterFactory.createMethodArgumentParameter(
                                                    nextProbeIndex, replayData, 0, null);
                                            containedParameterIterator.set(returnParam);

                                            identifyIteratorScanRequest.addListener(returnParam.getProb()
                                                            .getValue()
                                                    , this);
                                            iteratorProbe.set(index);

                                            break;
                                        case "next":
                                            if (iteratorProbe.get() == -1) {
                                                return;
                                            }
                                            if (event.getValue() == containedParameterIterator.get()
                                                    .getProb()
                                                    .getValue()) {
                                                int nextProbeIndex1 = replayData.getNextProbeIndex(index);
                                                callReturnProbe = replayData.getDataEvents()
                                                        .get(nextProbeIndex1);
                                                callReturnProbeInfo = replayData.getProbeInfo(
                                                        callReturnProbe.getDataId());
                                                assert callReturnProbeInfo.getEventType() == EventType.CALL_RETURN;
                                                nextValueProbe.set(index);

                                                containedParameter = ParameterFactory.createMethodArgumentParameter(
                                                        nextProbeIndex1, replayData, 0, null
                                                );
                                                identifyIteratorScanRequest.addListener(callReturnProbe.getValue(),
                                                        this);
                                                nextValueParameter.set(containedParameter);

                                            }
                                    }
                                } else if (probeInfo.getEventType() == EventType.LOCAL_STORE || probeInfo.getEventType() == EventType.LOCAL_LOAD) {
                                    if (nextValueParameter.get() == null) {
                                        return;
                                    }
                                    if (event.getValue() != nextValueParameter.get()
                                            .getProb()
                                            .getValue()) {
                                        return;
                                    }
                                    Parameter paramWithNameAndType = ParameterFactory.createMethodArgumentParameter(
                                            index, replayData, 0, null
                                    );
                                    if (paramWithNameAndType.getName()
                                            .startsWith("\\(")) {
                                        return;
                                    }
                                    if (paramWithNameAndType.getType() != null) {
                                        Parameter existingValue = nextValueParameter.get();
                                        paramWithNameAndType.getProb()
                                                .setSerializedValue(
                                                        existingValue.getProb()
                                                                .getSerializedValue()
                                                );
                                        nextValueParameter.set(paramWithNameAndType);
                                        identifyIteratorScanRequest.abort();
                                    }
                                }
                            }
                        });

                replayData.ScanForValue(identifyIteratorScanRequest);
                Parameter nextValueParam = nextValueParameter.get();
                targetParameter.setTemplateParameter("E", nextValueParam);

                return null;
            default:
                break;
        }
        return null;
    }
}
