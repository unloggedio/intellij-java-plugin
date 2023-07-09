package com.insidious.plugin.callbacks;

import com.insidious.plugin.pojo.atomic.StoredCandidate;

public interface CandidateLifeListener {
    public void onSaved(StoredCandidate storedCandidate);

    public void onSaveRequest(StoredCandidate storedCandidate);

    public void onDeleteRequest(StoredCandidate storedCandidate);

    public void onDeleted(StoredCandidate storedCandidate);

    public void onUpdated(StoredCandidate storedCandidate);

    public void onUpdateRequest(StoredCandidate storedCandidate);

    String getSaveLocation();
}
