package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;

public interface CandidateSelectedListener {
    void onCandidateSelected(TestCandidateMetadata testCandidateMetadata);
}
