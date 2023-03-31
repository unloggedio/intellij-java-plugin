package com.insidious.plugin.agent;

public interface ExecutionResponseListener {
    void onExecutionComplete(AgentCommandRequest agentCommandRequest, AgentCommandResponse agentCommandResponse);
}
