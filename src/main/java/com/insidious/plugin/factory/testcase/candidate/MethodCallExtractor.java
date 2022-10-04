package com.insidious.plugin.factory.testcase.candidate;

import com.insidious.common.weaver.ClassInfo;
import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.MethodInfo;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.pojo.EventMatchListener;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

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
        String returnClassName = ClassTypeUtils.getDottedClassName(returnType);
        if (ownerClassName.equals(returnClassName)) {
            // highly likely that this is a builder call
            // so we want to avoid this object in future as well
            Parameter builderParameter = new Parameter();
            builderParameter.setValue(event.getValue());
            variableContainer.add(builderParameter);
            return;
        }


        if (existingVariable != null) {
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

        // happens when the function call is on the return value of another function call
        if (methodCallExpression.getSubject().getName() == null) return;
        LoggerUtil.logEvent("SearchCallSubject", 0, index, event, probeInfo, classInfo, methodInfo);
        callList.add(methodCallExpression);
    }

}
