package com.insidious.plugin.ui;

import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.ResourceEmbedMode;
import com.insidious.plugin.pojo.frameworks.JsonFramework;
import com.insidious.plugin.pojo.frameworks.MockFramework;
import com.insidious.plugin.pojo.frameworks.TestFramework;
import com.squareup.javapoet.ClassName;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The input configuration for generating a test case script
 * this contains
 * - the candidates to be included in the script
 * - the calls to be mocked for each candidate
 * - the test framework to be used
 * - the json framework to be used
 */
public class TestCaseGenerationConfiguration {


    private final Set<MethodCallExpression> callExpressionList = new HashSet<>();
    private TestFramework testFramework;
    private MockFramework mockFramework;
    private JsonFramework jsonFramework;
    private ResourceEmbedMode resourceEmbedMode;
    private String testMethodName;
    private boolean useMockitoAnnotations;
    private List<TestCandidateMetadata> testCandidateMetadataList = new LinkedList<>();
    private boolean addFieldMocksCheckBox;

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

    public TestCaseGenerationConfiguration(TestCaseGenerationConfiguration original) {

        testFramework = original.testFramework;
        mockFramework = original.mockFramework;
        jsonFramework = original.jsonFramework;
        resourceEmbedMode = original.resourceEmbedMode;

        testMethodName = original.testMethodName;
        callExpressionList.addAll(original.callExpressionList
                .stream()
                .map(MethodCallExpression::new)
                .collect(Collectors.toSet()));

        useMockitoAnnotations = original.useMockitoAnnotations;

        testCandidateMetadataList = original.testCandidateMetadataList
                .stream()
                .map(TestCandidateMetadata::new)
                .collect(Collectors.toList());
    }

    public MockFramework getMockFramework() {
        return mockFramework;
    }

    public void setMockFramework(MockFramework mockFramework) {
        this.mockFramework = mockFramework;
    }

    public List<TestCandidateMetadata> getTestCandidateMetadataList() {
        return testCandidateMetadataList;
    }

    public void setTestCandidateMetadataList(List<TestCandidateMetadata> testCandidateMetadataList) {
        this.testCandidateMetadataList = testCandidateMetadataList;
    }

    public Set<MethodCallExpression> getCallExpressionList() {
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

//    public void setTestFramework(TestFramework testFramework) {
//        this.testFramework = testFramework;
//    }

    public void setJsonFramework(JsonFramework jsonFramework) {
        this.jsonFramework = jsonFramework;
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

    public void setResourceEmbedMode(ResourceEmbedMode resourceEmbedMode) {
        this.resourceEmbedMode = resourceEmbedMode;
    }

    public ClassName getTestBeforeAnnotationType() {
        return testFramework.getBeforeAnnotationType();
    }

    public ClassName getTestAfterAnnotationType() {
        return testFramework.getAfterAnnotationType();
    }

    public ClassName getTestAnnotationType() {
        return testFramework.getTestAnnotationType();
    }

    public String getTestMethodName() {
        return testMethodName;
    }

    public void setTestMethodName(String text) {
        this.testMethodName = text;
    }

    public void setUseMockitoAnnotations(boolean useMockitoAnnotations) {
        this.useMockitoAnnotations = useMockitoAnnotations;
    }

    public boolean useMockitoAnnotations() {
        return useMockitoAnnotations;
    }

    public boolean isAddFieldMocksCheckBox() {
        return addFieldMocksCheckBox;
    }

    public void setAddFieldMocksCheckBox(boolean addFieldMocksCheckBox) {
        this.addFieldMocksCheckBox = addFieldMocksCheckBox;
    }
}
