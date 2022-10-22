package com.insidious.plugin.ui;

import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;

public interface TestGenerateActionListener {
    void generateTestCase(TestCandidateMetadata testCandidateMetadata, TestCaseGenerationConfiguration generationConfiguration);
    void cancel();
}
