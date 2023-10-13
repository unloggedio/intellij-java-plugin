package com.insidious.plugin.callbacks;

import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.pojo.atomic.ClassUnderTest;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.ui.methodscope.AgentCommandResponseListener;
import com.intellij.openapi.project.Project;

import java.awt.*;
import java.util.List;

public interface CandidateLifeListener {
    void executeCandidate(
            List<StoredCandidate> metadata,
            ClassUnderTest classUnderTest,
            String source,
            AgentCommandResponseListener<String> stringAgentCommandResponseListener
    );

    void displayResponse(Component responseComponent);

    void onSaved(StoredCandidate storedCandidate);

    void onSaveRequest(StoredCandidate storedCandidate, AgentCommandResponse<String> agentCommandResponse);

    void onDeleteRequest(StoredCandidate storedCandidate);

    void onDeleted(StoredCandidate storedCandidate);

    void onUpdated(StoredCandidate storedCandidate);

    void onUpdateRequest(StoredCandidate storedCandidate);

    void onGenerateJunitTestCaseRequest(StoredCandidate storedCandidate);

    void onCandidateSelected(StoredCandidate testCandidateMetadata);

    void onCancel();

    Project getProject();
}
