package com.insidious.plugin.ui;

import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;

public interface TestGenerateActionListener {
    void generateTestCase(TestCaseGenerationConfiguration generationConfiguration);
    void cancel();
}
