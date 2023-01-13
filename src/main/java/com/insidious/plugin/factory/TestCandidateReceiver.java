package com.insidious.plugin.factory;

import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;

public interface TestCandidateReceiver {

    void handleTestCandidate(TestCandidateMetadata testCandidateMetadata);
}
