package com.insidious.plugin.ui;

import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;

import java.io.IOException;

public interface TestGenerateActionListener {
    void generateTestCase(TestCaseGenerationConfiguration generationConfiguration) throws Exception;
    void cancel();
}
