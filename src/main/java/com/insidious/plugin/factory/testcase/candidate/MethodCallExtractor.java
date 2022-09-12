package com.insidious.plugin.factory.testcase.candidate;

import com.insidious.common.weaver.*;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.extension.model.DirectionType;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.extension.model.ScanResult;
import com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.factory.testcase.parameter.ParameterFactory;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.pojo.*;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.Nullable;

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
            return;
        }

        if (methodName.equals("<init>")) {
            return;
        }

        MethodCallExpression methodCallExpression = replayData.extractMethodCall(index);
        if (methodCallExpression == null) return;
        callList.add(methodCallExpression);
    }

}
