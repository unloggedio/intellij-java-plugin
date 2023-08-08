package com.insidious.plugin.callbacks;

import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.intellij.openapi.project.Project;

public interface CandidateLifeListener {
    void onSaved(StoredCandidate storedCandidate);

    void onSaveRequest(StoredCandidate storedCandidate, AgentCommandResponse<String> agentCommandResponse);

    void onDeleteRequest(StoredCandidate storedCandidate);

    void onDeleted(StoredCandidate storedCandidate);

    void onUpdated(StoredCandidate storedCandidate);

    void onUpdateRequest(StoredCandidate storedCandidate);
    
    void onCancel();

    String getSaveLocation();

    Project getProject();
}
