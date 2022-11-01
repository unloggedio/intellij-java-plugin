package com.insidious.plugin.ui;

import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.JsonFramework;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.ResourceEmbedMode;
import com.insidious.plugin.pojo.TestFramework;

import java.util.LinkedList;
import java.util.List;

/**
 * The input configuration for generating a test case script
 * this contains
 * - the candidates to be included in the script
 * - the calls to be mocked for each candidate
 * - the test framework to be used
 * - the json framework to be used
 */
public class TestCaseGenerationConfiguration {


    private final List<TestCandidateMetadata> testCandidateMetadataList = new LinkedList<>();
    private final List<MethodCallExpression> callExpressionList = new LinkedList<>();
    private TestFramework testFramework = TestFramework.JUNIT5;
    private JsonFramework jsonFramework = JsonFramework.GSON;

    private ResourceEmbedMode resourceEmbedMode = ResourceEmbedMode.IN_FILE;

    public TestCaseGenerationConfiguration() {
    }

    public List<TestCandidateMetadata> getTestCandidateMetadataList() {
        return testCandidateMetadataList;
    }

    public List<MethodCallExpression> getCallExpressionList() {
        return callExpressionList;
    }

    public TestFramework getTestFramework() {
        return testFramework;
    }

    public void setTestFramework(TestFramework testFramework) {
        this.testFramework = testFramework;
    }

    public JsonFramework getJsonFramework() {
        return jsonFramework;
    }

    public void setJsonFramework(JsonFramework jsonFramework) {
        this.jsonFramework = jsonFramework;
    }

    public ResourceEmbedMode getResourceEmbedMode() {
        return resourceEmbedMode;
    }

    public void setResourceEmbedMode(ResourceEmbedMode resourceEmbedMode) {
        this.resourceEmbedMode = resourceEmbedMode;
    }
}
