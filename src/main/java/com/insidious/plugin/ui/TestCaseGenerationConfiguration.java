package com.insidious.plugin.ui;

import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.*;
import com.squareup.javapoet.ClassName;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * The input configuration for generating a test case script
 * this contains
 * - the candidates to be included in the script
 * - the calls to be mocked for each candidate
 * - the test framework to be used
 * - the json framework to be used
 */
public class TestCaseGenerationConfiguration {


    private final TestFramework testFramework;
    private String testName;

    public MockFramework getMockFramework() {
        return mockFramework;
    }

    private final MockFramework mockFramework;
    private final JsonFramework jsonFramework;
    private final ResourceEmbedMode resourceEmbedMode;
    private List<TestCandidateMetadata> testCandidateMetadataList = new LinkedList<>();
    private final Set<MethodCallExpression> callExpressionList = new HashSet<>();

    public TestCaseGenerationConfiguration(
            TestFramework testFramework,
            MockFramework mockFramework,
            JsonFramework jsonFramework,
            ResourceEmbedMode resourceEmbedMode
    ) {
        this.testFramework = testFramework;
        this.mockFramework = mockFramework;
        this.jsonFramework = jsonFramework;
        this.resourceEmbedMode = resourceEmbedMode;
    }

    public List<TestCandidateMetadata> getTestCandidateMetadataList() {
        return testCandidateMetadataList;
    }

    public Set<MethodCallExpression> getCallExpressionList() {
        return callExpressionList;
    }

    public TestFramework getTestFramework() {
        return testFramework;
    }

//    public void setTestFramework(TestFramework testFramework) {
//        this.testFramework = testFramework;
//    }

    public JsonFramework getJsonFramework() {
        return jsonFramework;
    }

//    public void setJsonFramework(JsonFramework jsonFramework) {
//        this.jsonFramework = jsonFramework;
//    }

    public ResourceEmbedMode getResourceEmbedMode() {
        return resourceEmbedMode;
    }

//    public void setResourceEmbedMode(ResourceEmbedMode resourceEmbedMode) {
//        this.resourceEmbedMode = resourceEmbedMode;
//    }

    public ClassName getTestBeforeAnnotationType() {
        return testFramework.getBeforeAnnotationType();
    }
    public ClassName getTestAfterAnnotationType() {
        return testFramework.getAfterAnnotationType();
    }

    public ClassName getTestAnnotationType() {
        return testFramework.getTestAnnotationType();
    }

    public void setTestCandidateMetadataList(List<TestCandidateMetadata> testCandidateMetadataList) {
        this.testCandidateMetadataList = testCandidateMetadataList;
    }

    public void setTestName(String text) {
        this.testName = text;
    }

    public String getTestMethodName() {
        return testName;
    }
}
