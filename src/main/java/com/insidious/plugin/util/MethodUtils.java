package com.insidious.plugin.util;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.ParameterAdapter;
import com.insidious.plugin.agent.AgentCommand;
import com.insidious.plugin.agent.AgentCommandRequest;
import com.insidious.plugin.agent.AgentCommandRequestType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;

import java.util.List;

public class MethodUtils {
    public static AgentCommandRequest createRequestWithParameters(
            MethodAdapter methodAdapter,
            PsiClass psiClass,
            List<String> parameterValues
    ) {

        AgentCommandRequest agentCommandRequest = new AgentCommandRequest();
        agentCommandRequest.setCommand(AgentCommand.EXECUTE);

        String qualifiedName = ApplicationManager.getApplication().runReadAction(
                (Computable<String>) () -> {
                    agentCommandRequest.setMethodSignature(methodAdapter.getJVMSignature());
                    agentCommandRequest.setClassName(psiClass.getQualifiedName());
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
        agentCommandRequest.setMethodParameters(parameterValues);
        agentCommandRequest.setRequestType(AgentCommandRequestType.REPEAT_INVOKE);
        return agentCommandRequest;
    }

}
