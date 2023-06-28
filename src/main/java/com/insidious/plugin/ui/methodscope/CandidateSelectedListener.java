package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.atomic.StoredCandidate;

public interface CandidateSelectedListener {
    void onCandidateSelected(StoredCandidate testCandidateMetadata);
}
