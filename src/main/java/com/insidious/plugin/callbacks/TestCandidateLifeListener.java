package com.insidious.plugin.callbacks;

import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.atomic.ClassUnderTest;
import com.insidious.plugin.ui.methodscope.AgentCommandResponseListener;

import java.awt.*;
import java.util.List;

public interface TestCandidateLifeListener {
    void executeCandidate(
            List<TestCandidateMetadata> metadata,
            ClassUnderTest classUnderTest,
            ExecutionRequestSourceType source,
            AgentCommandResponseListener<TestCandidateMetadata, String> responseListener
    );

    void displayResponse(Component responseComponent, boolean isExceptionFlow);

    void onSaved(TestCandidateMetadata storedCandidate);

    void onSelected(TestCandidateMetadata storedCandidate);

    void unSelected(TestCandidateMetadata storedCandidate);

    void onSaveRequest(TestCandidateMetadata storedCandidate, AgentCommandResponse<String> agentCommandResponse);

    void onDeleteRequest(TestCandidateMetadata storedCandidate);

    void onDeleted(TestCandidateMetadata storedCandidate);

    void onUpdated(TestCandidateMetadata storedCandidate);

    void onUpdateRequest(TestCandidateMetadata storedCandidate);

    void onGenerateJunitTestCaseRequest(TestCandidateMetadata storedCandidate);

    void onCandidateSelected(TestCandidateMetadata testCandidateMetadata);

    void onCancel();

    void onExpandChildren(TestCandidateMetadata candidateMetadata);
}
