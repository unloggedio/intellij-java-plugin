package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.atomic.StoredCandidate;

public interface AgentCommandResponseListener<T> {
    void onSuccess(StoredCandidate testCandidate, AgentCommandResponse<T> agentCommandResponse, DifferenceResult diffResult);
}
