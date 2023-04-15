package com.insidious.plugin.util;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.agent.AgentCommand;
import com.insidious.plugin.agent.AgentCommandRequest;

import java.util.List;

public class MethodUtils {
    public static AgentCommandRequest createRequestWithParameters(MethodAdapter methodAdapter,
                                                                  List<String> parameterValues) {

        String methodJniSignature = methodAdapter.getJVMSignature();
        AgentCommandRequest agentCommandRequest = new AgentCommandRequest();
        agentCommandRequest.setCommand(AgentCommand.EXECUTE);
        agentCommandRequest.setClassName(methodAdapter.getContainingClass().getQualifiedName());
        agentCommandRequest.setMethodName(methodAdapter.getName());
        agentCommandRequest.setMethodSignature(methodJniSignature);
        agentCommandRequest.setMethodParameters(parameterValues);
        return agentCommandRequest;
    }

}
