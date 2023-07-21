package com.insidious.plugin.callbacks;

import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.intellij.openapi.project.Project;

public interface CandidateLifeListener {
    public void onSaved(StoredCandidate storedCandidate);

    public void onSaveRequest(StoredCandidate storedCandidate, AgentCommandResponse<String> agentCommandResponse);

    public void onDeleteRequest(StoredCandidate storedCandidate);

    public void onDeleted(StoredCandidate storedCandidate);

    public void onUpdated(StoredCandidate storedCandidate);

    public void onUpdateRequest(StoredCandidate storedCandidate);

    String getSaveLocation();
    public Project getProject();
}
