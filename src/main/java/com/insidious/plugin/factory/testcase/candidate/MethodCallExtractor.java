package com.insidious.plugin.factory.testcase.candidate;

import com.insidious.common.weaver.*;
import com.insidious.plugin.client.exception.SessionNotSelectedException;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.extension.model.DirectionType;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.extension.model.ScanResult;
import com.insidious.plugin.factory.testcase.TestCaseRequest;
import com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.factory.testcase.parameter.ParameterFactory;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.pojo.*;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MethodCallExtractor implements EventMatchListener {


    private final ReplayData replayData;
    private final VariableContainer variableContainer;
    private final List<String> typeHierarchy;
    private final Logger logger = LoggerUtil.getInstance(MethodCallExtractor.class);
    private final List<String> noMockClassList;
    private final List<MethodCallExpression> callList = new LinkedList<>();

    public MethodCallExtractor(
            ReplayData replayData,
            VariableContainer variableContainer,
            List<String> typeHierarchy,
            List<String> noMockClassList) {
        this.replayData = replayData;
        this.variableContainer = variableContainer;
        this.typeHierarchy = typeHierarchy;
        this.noMockClassList = noMockClassList;
    }

    public List<MethodCallExpression> getCallList() {
        return callList;
    }

    @Override
    public void eventMatched(Integer index, int matchedStack) {
        DataEventWithSessionId event = replayData.getDataEvents().get(index);
        Optional<Parameter> existingVariable = Optional.empty();
        if (event.getValue() != 0) {
            existingVariable = variableContainer.getParametersById(String.valueOf(event.getValue()));
        }

        DataInfo probeInfo = replayData.getProbeInfo(event.getDataId());
        String methodName = probeInfo.getAttribute("Name", null);
        String methodDescription = probeInfo.getAttribute("Desc", null);
        List<String> methodDescList = ClassTypeUtils.splitMethodDesc(methodDescription);
        String returnType = methodDescList.get(methodDescList.size() - 1);


        String ownerClass = probeInfo.getAttribute("Owner", null);
        String instruction = probeInfo.getAttribute("Instruction", null);

        ClassInfo classInfo = replayData.getClassInfo(probeInfo.getClassId());
        MethodInfo methodInfo = replayData.getMethodInfo(probeInfo.getMethodId());

        String ownerClassName = ClassTypeUtils.getDottedClassName(ownerClass);


        LoggerUtil.logEvent(
                "SearchCallSubject", 0, index, event, probeInfo, classInfo, methodInfo
        );


        if (existingVariable.isPresent()) {
            return;
        }

        if (Objects.equals(returnType, "V")) {
            return;
        }


        if (typeHierarchy.contains(ownerClassName)) {
            return;
        }

        if (noMockClassList.size() > 0) {
            for (String noMock : noMockClassList) {
                if (ownerClassName.startsWith(noMock)) {
                    return;
                }
            }
        }

        if (ownerClass.startsWith("reactor/core")) {
            return;
        }

        if (ownerClass.startsWith("org/apache/commons")) {
            return;
        }

        if (ownerClass.startsWith("java/")) {
            return;
        }

        // this should be in some config
        if (ownerClass.contains("/slf4j/")) {
            return;
        }
        if (instruction.equals("INVOKESTATIC")) {
            LoggerUtil.logEvent(
                    "STATICCallSubject", 0, index, event, probeInfo, classInfo, methodInfo
            );


            try {
                TestCandidateMetadata candidate = TestCandidateMetadata.create(
                        typeHierarchy, methodInfo, index, replayData
                );
            } catch (APICallException | SessionNotSelectedException e) {
                logger.warn("failed to extract calls for static call: " + methodInfo);
//                throw new RuntimeException(e);
            }
            return;
        }

        if (methodName.equals("<init>")) {
            return;
        }

        List<String> subjectTypeHierarchy = replayData.buildHierarchyFromTypeName(
                "L" + ownerClass + ";");

        String callOwner = probeInfo.getAttribute("Owner", null);
        if (callOwner == null) {
            return;
        }

        logger.warn("[MethodCall] " + ownerClass + "." + methodName + " : " + methodDescription);

        List<Parameter> callArguments = new LinkedList<>();

        ScanRequest callSubjectScan = new ScanRequest(new ScanResult(index, 0), 0,
                DirectionType.BACKWARDS);
        AtomicInteger subjectMatchIndex = new AtomicInteger(0);
        List<Parameter> subjectParameterList = new LinkedList<>();
        EventMatchListener subjectMatchListener = new EventMatchListener() {
            @Override
            public void eventMatched(Integer index, int matchedStack) {

                DataEventWithSessionId matchedSubjectEvent = replayData.getDataEvents().get(index);
                DataInfo subjectEventProbe = replayData.getProbeInfo(matchedSubjectEvent.getDataId());

                String parameterClassType = subjectEventProbe.getAttribute("Type", null);

                Parameter potentialSubjectParameter;

                potentialSubjectParameter =
                        ParameterFactory.createParameter(index, replayData, 0, parameterClassType);

                List<String> buildHierarchyFromTypeName =
                        replayData.buildHierarchyFromTypeName(
                                ClassTypeUtils.getDescriptorName(
                                        potentialSubjectParameter.getType()));

                if (buildHierarchyFromTypeName.contains(subjectTypeHierarchy.get(0))) {
                    subjectParameterList.add(potentialSubjectParameter);
                }
            }
        };
//                callSubjectScan.addListener(EventType.CALL_RETURN, subjectMatchListener);
//                callSubjectScan.addListener(EventType.LOCAL_LOAD, subjectMatchListener);
        callSubjectScan.addListener(EventType.GET_INSTANCE_FIELD_RESULT, subjectMatchListener);
        callSubjectScan.addListener(EventType.GET_STATIC_FIELD, subjectMatchListener);

        callSubjectScan.matchUntil(EventType.METHOD_ENTRY);
        replayData.eventScan(callSubjectScan);

        Parameter subjectParameter;
        if (subjectParameterList.size() == 0) {
            logger.warn("failed to identify subject for the call: " + methodName + " at " + probeInfo);
            return;
//                    subjectParameter = new Parameter();
//                    subjectParameter.setName("testSubjectFor" + methodName);
//                    subjectParameter.setType(subjectTypeHierarchy.get(0));
        } else {
            subjectParameter = subjectParameterList.get(0);
        }


        ScanRequest callReturnScan = new ScanRequest(new ScanResult(index, 0), 0,
                DirectionType.FORWARDS);

        AtomicInteger lookingForParams = new AtomicInteger(1);
        callReturnScan.addListener(EventType.CALL_PARAM, new EventMatchListener() {
            @Override
            public void eventMatched(Integer index, int matchedStack) {
                if (lookingForParams.get() == 1) {
                    Parameter callArgumentParameter = ParameterFactory.createParameterByCallArgument(
                            index, replayData, callArguments.size(), null
                    );
                    callArguments.add(callArgumentParameter);
                }
            }
        });
        callReturnScan.addListener(EventType.CALL, (i, s) -> lookingForParams.set(0));
        callReturnScan.addListener(EventType.METHOD_ENTRY, (i, s) -> lookingForParams.set(0));


        callReturnScan.matchUntil(EventType.CALL_RETURN);
        callReturnScan.matchUntil(EventType.METHOD_EXCEPTIONAL_EXIT);

        ScanResult callReturnScanResult = replayData.eventScan(callReturnScan);
        if (callReturnScanResult.getIndex() == -1 ||
                callReturnScanResult.getIndex() == replayData.getDataEvents().size()) {
            logger.warn("call return not found for " + probeInfo);
            return;
        }

        DataInfo exitProbeInfo = replayData.getProbeInfo(replayData.getDataEvents().get(callReturnScanResult.getIndex()).getDataId());
        Parameter callReturnParameter = null;
        Parameter exception = null;


        if (exitProbeInfo.getEventType() == EventType.METHOD_EXCEPTIONAL_EXIT) {

            DataEventWithSessionId extEvent = replayData.getDataEvents().get(callReturnScanResult.getIndex());
            ObjectInfo exitValueObjectInfo = replayData.getObjectInfo(extEvent.getValue());
            TypeInfo exceptionTypeInfo = replayData.getTypeInfo(exitValueObjectInfo.getTypeId());

            exception = new Parameter();
            exception.setType(ClassTypeUtils.getDottedClassName(exceptionTypeInfo.getTypeNameFromClass()));
            exception.setValue(extEvent.getValue());
            exception.setProb(extEvent);
            exception.setProbeInfo(exitProbeInfo);

        } else if (exitProbeInfo.getEventType() == EventType.CALL_RETURN) {
            callReturnParameter = ParameterFactory.createReturnValueParameter(
                    callReturnScanResult.getIndex(), replayData, returnType);

            if (callReturnParameter.getName() == null) {
                callReturnParameter.setName(
                        ClassTypeUtils.createVariableNameFromMethodName(methodName,
                                callReturnParameter.getType())
                );
            }

            if (callReturnParameter.getType() == null || callReturnParameter.getType().equals("V")) {
                return;
            }

        } else {
            logger.error("what kind of exit");
        }


        MethodCallExpression methodCallExpression = MethodCallExpressionFactory.MethodCallExpression(
                methodName, subjectParameter, VariableContainer.from(callArguments),
                callReturnParameter, exception);
        callList.add(methodCallExpression);
    }

}
