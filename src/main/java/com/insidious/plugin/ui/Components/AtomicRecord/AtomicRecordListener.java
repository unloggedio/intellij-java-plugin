package com.insidious.plugin.ui.Components.AtomicRecord;

import com.insidious.plugin.pojo.atomic.StoredCandidate;

public interface AtomicRecordListener {

    void triggerRecordAddition(String name, String description, StoredCandidate.AssertionType type);

    void deleteCandidateRecord(String candidateID);

    String getSaveLocation();
}
