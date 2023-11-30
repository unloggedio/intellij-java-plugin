package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.agent.AgentCommandResponse;

public interface AgentCommandResponseListener<C, T> {
    void onSuccess(C testCandidate, AgentCommandResponse<T> agentCommandResponse, DifferenceResult diffResult);
}
