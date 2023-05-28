package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;

public interface AgentCommandResponseListener<T> {
    void onSuccess(TestCandidateMetadata testCandidate, AgentCommandResponse<T> agentCommandResponse, DifferenceResult diffResult);
}
