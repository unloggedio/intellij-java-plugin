package com.insidious.plugin.factory;

import com.insidious.common.weaver.ClassInfo;
import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.EventType;
import com.insidious.common.weaver.MethodInfo;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.extension.model.DirectionType;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.extension.model.ScanResult;
import com.insidious.plugin.pojo.EventTypeMatchListener;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.ScanRequest;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class MethodCallExtractor implements EventTypeMatchListener {


    private final ReplayData replayData;
    private final VariableContainer variableContainer;
    private final List<String> typeHierarchy;
    private final Logger logger = LoggerUtil.getInstance(MethodCallExtractor.class);
    private List<String> noMockClassList;
    private List<MethodCallExpression> callList = new LinkedList<>();

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
    public void eventMatched(Integer index) {
        DataEventWithSessionId event = replayData.getDataEvents().get(index);
        Optional<Parameter> existingVariable = variableContainer.getParametersById(String.valueOf(event.getValue()));
        if (existingVariable.isPresent()) {
            return;
        }

        DataInfo probeInfo = replayData.getProbeInfo(event.getDataId());
        String methodName = probeInfo.getAttribute("Name", null);
        String methodDescription = probeInfo.getAttribute("Desc", null);
        List<String> methodDescList = ClassTypeUtils.splitMethodDesc(methodDescription);
        String returnType = methodDescList.get(methodDescList.size() - 1);
        if (Objects.equals(returnType, "V")) {
            return;
        }


        String ownerClass = probeInfo.getAttribute("Owner", null);
        String instruction = probeInfo.getAttribute("Instruction", null);

        ClassInfo classInfo = replayData.getClassInfo(probeInfo.getClassId());
        MethodInfo methodInfo = replayData.getMethodInfo(probeInfo.getMethodId());

        String dottedClassName = ClassTypeUtils.getDottedClassName(ownerClass);

        if (typeHierarchy.contains(dottedClassName)) {
            return;
        }

        if (noMockClassList.size() > 0) {
            for (String noMock : noMockClassList) {
                if (dottedClassName.startsWith(noMock)) {
                    return;
                }
            }
        }

        if (ownerClass.startsWith("reactor/core")) {
            return;
        }

        if (ownerClass.startsWith("java/")) {
            return;
        }
        if (ownerClass.contains("/slf4j/")) {
            return;
        }
        if (instruction.equals("INVOKESTATIC")) {
            return;
        }
        if (methodName.equals("<init>")) {
            return;
        }
        LoggerUtil.logEvent(
                "SearchCallSubject", 0, index, event, probeInfo, classInfo, methodInfo
        );

        List<String> subjectTypeHierarchy = replayData.buildHierarchyFromTypeName(
                "L" + ownerClass + ";");

        logger.warn("potential call: " + probeInfo.getAttributes());
        String callOwner = probeInfo.getAttribute("Owner", null);
        if (callOwner == null) {
            return;
        }

        List<Parameter> callArguments = new LinkedList<>();

        ScanRequest callSubjectScan = new ScanRequest(new ScanResult(index, 0), 0,
                DirectionType.BACKWARDS);
        AtomicInteger subjectMatchIndex = new AtomicInteger(0);
        List<Parameter> subjectParameterList = new LinkedList<>();
        EventTypeMatchListener subjectMatchListener = new EventTypeMatchListener() {
            @Override
            public void eventMatched(Integer index) {

                DataEventWithSessionId matchedSubjectEvent =
                        replayData.getDataEvents().get(index);
                DataInfo subjectEventProbe = replayData.getProbeInfo(matchedSubjectEvent.getDataId());

                String valueType = subjectEventProbe.getAttribute("Type", null);

                Parameter potentialSubjectParameter;

                potentialSubjectParameter =
                        ParameterFactory.createParameter(index, replayData,
                                0, valueType);

                List<String> buildHierarchyFromTypeName =
                        replayData.buildHierarchyFromTypeName(ClassTypeUtils.getDescriptorName(potentialSubjectParameter.getType()));
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
        callReturnScan.addListener(EventType.CALL_PARAM, new EventTypeMatchListener() {
            @Override
            public void eventMatched(Integer index) {
                if (lookingForParams.get() == 1) {
                    Parameter callArgumentParameter = ParameterFactory.createCallArgumentParameter(
                            index, replayData, callArguments.size(), null
                    );
                    callArguments.add(callArgumentParameter);
                }
            }
        });
        callReturnScan.addListener(EventType.CALL, i -> lookingForParams.set(0));
        callReturnScan.addListener(EventType.METHOD_ENTRY, i -> lookingForParams.set(0));


        callReturnScan.matchUntil(EventType.CALL_RETURN);

        ScanResult callReturnScanResult = replayData.eventScan(callReturnScan);
        if (callReturnScanResult.getIndex() == -1 ||
                callReturnScanResult.getIndex() == replayData.getDataEvents().size()) {
            logger.warn("call return not found for " + probeInfo);
            return;
        }

        Parameter callReturnParameter =
                ParameterFactory.createReturnValueParameter(
                        callReturnScanResult.getIndex(), replayData, returnType);

        if (callReturnParameter.getType() == null ||
                callReturnParameter.getType().equals("V")) {
            return;
        }


        MethodCallExpression methodCallExpression = new MethodCallExpression(
                methodName, subjectParameter, callArguments, callReturnParameter
        );
        callList.add(methodCallExpression);
    }

}
