package com.insidious.plugin.factory.testcase.candidate;

import com.insidious.common.weaver.*;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.extension.model.DirectionType;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.extension.model.ScanResult;
import com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory;
import com.insidious.plugin.factory.testcase.parameter.ParameterFactory;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.pojo.EventMatchListener;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.ScanRequest;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class StaticMethodCallExtractor implements EventMatchListener {


    private final ReplayData replayData;
    private final VariableContainer variableContainer;
    private final List<String> typeHierarchy;
    private final Logger logger = LoggerUtil.getInstance(StaticMethodCallExtractor.class);
    private final List<String> noMockClassList;
    private final LinkedList<MethodCallExpression> callsToMock = new LinkedList<>();

    public StaticMethodCallExtractor(
            ReplayData replayData,
            VariableContainer variableContainer,
            List<String> typeHierarchy,
            List<String> noMockClassList) {
        this.replayData = replayData;
        this.variableContainer = variableContainer;
        this.typeHierarchy = typeHierarchy;
        this.noMockClassList = noMockClassList;
    }


    public LinkedList<MethodCallExpression> getCallList() {
        return callsToMock;
    }

    @Override
    public void eventMatched(Integer index, int matchedStack) {
        DataEventWithSessionId event = replayData.getDataEvents().get(index);
        Parameter existingVariable = null;
        if (event.getValue() != 0) {
            existingVariable = variableContainer.getParametersById(event.getValue());
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


        if (!instruction.equals("INVOKESTATIC")) {
            return;
        }

        LoggerUtil.logEvent(
                "SearchCallSubject", 0, index, event, probeInfo, classInfo, methodInfo
        );


        if (ownerClass.startsWith("reactor/core")) {
            return;
        }

        if (ownerClass.startsWith("org/apache/commons")) {
            return;
        }

        if (ownerClass.startsWith("java/")) {
            return;
        }

        if (ownerClass.startsWith("javax/")) {
            return;
        }

        // this should be in some config
        if (ownerClass.contains("/slf4j/")) {
            return;
        }


        String callOwner = probeInfo.getAttribute("Owner", null);
        if (callOwner == null) {
            return;
        }

        logger.warn("[StaticMethodCall] " + ownerClass + "." + methodName + " : " + methodDescription);


        List<Parameter> callArguments = new LinkedList<>();

        ScanRequest methodEntryPointScan = new ScanRequest(
                new ScanResult(index, 0, false), 0, DirectionType.FORWARDS);
        methodEntryPointScan.matchUntil(EventType.CALL_RETURN);
        methodEntryPointScan.matchUntil(EventType.METHOD_ENTRY);
        ScanResult methodEntryScanMatch = replayData.ScanForEvents(methodEntryPointScan);
        if (!methodEntryScanMatch.matched()) {
            return;
        }
        DataEventWithSessionId methodEntryEvent = replayData.getDataEvents().get(methodEntryScanMatch.getIndex());
        DataInfo methodEntryProbeInfo = replayData.getProbeInfo(methodEntryEvent.getDataId());
        if (methodEntryProbeInfo.getEventType() == EventType.CALL_RETURN) {
            // this is a thirdparty library call which was not probed
            //
            return;
        }


        ScanRequest callsInsideStaticMethodCall = new ScanRequest(
                new ScanResult(methodEntryScanMatch.getIndex(), 0), ScanRequest.CURRENT_CLASS, DirectionType.FORWARDS
        );
        LinkedList<MethodCallExpression> callsList = new LinkedList<>();

        callsInsideStaticMethodCall.addListener(EventType.CALL, new EventMatchListener() {
            @Override
            public void eventMatched(Integer index, int matchedStack) {
                MethodCallExpression methodCall = replayData.extractMethodCall(index);
                if (methodCall == null || methodCall.getMethodName().equals("<init>")) {
                    return;
                }
                Parameter callSubject = methodCall.getSubject();
                String subjectType = callSubject.getType();
                if (subjectType.contains("slf4j") ||
                        subjectType.contains("google") ||
                        subjectType.contains("logback") ||
                        subjectType.contains("hibernate") ||
                        subjectType.startsWith("javax.")
                ) {
                    return;
                }
                if (callSubject.getName() != null && callSubject.getName().contains("$")) {
                    return;
                }
                if (callSubject.getProbeInfo().getEventType() == EventType.GET_STATIC_FIELD) {
                    callsList.add(methodCall);
                }
            }
        });
        callsInsideStaticMethodCall.matchUntil(EventType.METHOD_NORMAL_EXIT);
        callsInsideStaticMethodCall.matchUntil(EventType.METHOD_EXCEPTIONAL_EXIT);
        replayData.ScanForEvents(callsInsideStaticMethodCall);

        if (callsList.size() > 0) {
            // at this point we know that this static method call uses some fields which were not
            // injected,
            MethodCallExpression methodCallExpression = replayData.extractMethodCall(index);
            Parameter staticParameter = new Parameter();
            staticParameter.setType(ownerClassName);
            staticParameter.setName(ClassTypeUtils.createVariableName(ownerClassName));
            staticParameter.setProb(event);
            staticParameter.setProbeInfo(probeInfo);
            methodCallExpression.setSubject(staticParameter);
            callsToMock.add(methodCallExpression);

        }
    }

}
