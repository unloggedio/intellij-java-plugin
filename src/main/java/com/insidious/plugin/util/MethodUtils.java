package com.insidious.plugin.util;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.ParameterAdapter;
import com.insidious.plugin.agent.AgentCommand;
import com.insidious.plugin.agent.AgentCommandRequest;
import com.insidious.plugin.agent.AgentCommandRequestType;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.atomic.ClassUnderTest;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;

import java.util.ArrayList;
import java.util.List;

public class MethodUtils {
    public static AgentCommandRequest createExecuteRequestWithParameters(
            MethodAdapter methodAdapter,
            ClassUnderTest classUnderTest,
            List<String> parameterValues,
            boolean processArgumentTypes,
            ArrayList<DeclaredMock> enabledMockId) {

        AgentCommandRequest agentCommandRequest = new AgentCommandRequest();
        agentCommandRequest.setCommand(AgentCommand.EXECUTE);

        agentCommandRequest.setDeclaredMocks(enabledMockId);

        String qualifiedName = ApplicationManager.getApplication().runReadAction(
                (Computable<String>) () -> {
                    agentCommandRequest.setMethodSignature(methodAdapter.getJVMSignature());
                    agentCommandRequest.setClassName(classUnderTest.getQualifiedClassName());
                    agentCommandRequest.setMethodName(methodAdapter.getName());
                    ParameterAdapter[] methodParameters = methodAdapter.getParameters();
                    String[] parameterCanonicalStrings = new String[methodParameters.length];
                    for (int i = 0; i < methodParameters.length; i++) {
                        ParameterAdapter methodParameter = methodParameters[i];
                        parameterCanonicalStrings[i] = methodParameter.getType().getCanonicalText();
                    }
                    agentCommandRequest.setParameterTypes(List.of(parameterCanonicalStrings));

                    return methodAdapter.getContainingClass().getQualifiedName();
                });

        if (processArgumentTypes && parameterValues != null) {
            ArrayList<String> convertedParameterValues = new ArrayList<>(parameterValues.size());
            List<String> parameterTypes = agentCommandRequest.getParameterTypes();
            for (int i = 0; i < parameterValues.size(); i++) {
                String parameterValue = parameterValues.get(i);
                String parameterType = parameterTypes.get(i);
                if (parameterType.equals("float")) {
                    parameterValue = String.valueOf(Float.intBitsToFloat(Integer.parseInt(parameterValue)));
                } else if (parameterType.equals("double")) {
                    parameterValue = String.valueOf(Double.longBitsToDouble(Long.parseLong(parameterValue)));
                }
                convertedParameterValues.add(parameterValue);
            }
            parameterValues = convertedParameterValues;
        }


        agentCommandRequest.setMethodParameters(parameterValues);
        agentCommandRequest.setRequestType(AgentCommandRequestType.REPEAT_INVOKE);
        return agentCommandRequest;
    }

}
