package com.insidious.plugin.callbacks;

import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.ReplayAllExecutionContext;
import com.insidious.plugin.pojo.atomic.ClassUnderTest;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.ui.library.DeclaredMockItemPanel;
import com.insidious.plugin.ui.methodscope.AgentCommandResponseListener;
import com.intellij.openapi.project.Project;

import java.awt.*;
import java.util.List;

public interface CandidateLifeListener {
    void executeCandidate(
            List<StoredCandidate> metadata,
            ClassUnderTest classUnderTest,
            ReplayAllExecutionContext context,
            AgentCommandResponseListener<TestCandidateMetadata, String> stringAgentCommandResponseListener
    );

    void displayResponse(Component responseComponent, boolean isExceptionFlow);

    void onSaved(StoredCandidate storedCandidate);


    void onSaveRequest(StoredCandidate storedCandidate, AgentCommandResponse<String> agentCommandResponse);

    void onDeleteRequest(StoredCandidate storedCandidate);

    void onDeleted(StoredCandidate storedCandidate);

    void onUpdated(StoredCandidate storedCandidate);

    void onUpdateRequest(StoredCandidate storedCandidate);

    void onGenerateJunitTestCaseRequest(StoredCandidate storedCandidate);

    void onCandidateSelected(StoredCandidate testCandidateMetadata);

    boolean canGenerateUnitCase(StoredCandidate candidate);

    void onCancel();

    Project getProject();

    void onSaved(DeclaredMock declaredMock);
}
