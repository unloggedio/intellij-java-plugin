package com.insidious.plugin.pojo;

import com.insidious.plugin.factory.TestCandidateMetadata;

public class TestCaseUnit {

    private final TestCandidateMetadata testCandidateMetadata;


    private final String code;

    public TestCaseUnit(TestCandidateMetadata testCandidateMetadata,
                        String code) {
        this.testCandidateMetadata = testCandidateMetadata;
        this.code = code;
    }

    @Override
    public String toString() {
        return code;
    }
}
