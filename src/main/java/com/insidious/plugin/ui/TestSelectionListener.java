package com.insidious.plugin.ui;

import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;

public interface TestSelectionListener {
    void onSelect(TestCandidateMetadata testCandidateMetadata);
}
