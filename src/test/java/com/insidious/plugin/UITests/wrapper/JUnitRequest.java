package com.insidious.plugin.UITests.wrapper;

import java.util.List;

public class JUnitRequest {

    private DirectInvokeRequest directInvokeRequest;
    private boolean executionOnDemand;
    private JunitOptions junitOption;
    private String testCasePath;
    private boolean isPreview;
    private boolean mockCalls;
    private boolean useMockitoAnnotation;
    private TestFrameworkOptions testFrameworkOption;
    private MockFrameworkOptions mockFrameworkOption;
    private JSONFrameworkOptions jsonFrameworkOption;
    private StoreOptions storeOption;

    public JUnitRequest(DirectInvokeRequest directInvokeRequest,
                        JunitOptions junitOption, String testCasePath, boolean executionOnDemand) {
        this.directInvokeRequest = directInvokeRequest;
        this.testCasePath = testCasePath;
        this.junitOption = junitOption;
        this.executionOnDemand = executionOnDemand;
    }

    public JUnitRequest(DirectInvokeRequest directInvokeRequest, JunitOptions junitOption,
                        String testCasePath, boolean isPreview,
                        boolean mockCalls, boolean useMockitoAnnotation,
                        TestFrameworkOptions testFrameworkOption, MockFrameworkOptions mockFrameworkOption,
                        JSONFrameworkOptions jsonFrameworkOption, StoreOptions storeOption, boolean executionOnDemand) {
        this.directInvokeRequest = directInvokeRequest;
        this.junitOption = junitOption;
        this.testCasePath = testCasePath;
        this.isPreview = isPreview;
        this.mockCalls = mockCalls;
        this.useMockitoAnnotation = useMockitoAnnotation;
        this.testFrameworkOption = testFrameworkOption;
        this.mockFrameworkOption = mockFrameworkOption;
        this.jsonFrameworkOption = jsonFrameworkOption;
        this.storeOption = storeOption;
        this.executionOnDemand = executionOnDemand;
    }

    public DirectInvokeRequest getDirectInvokeRequest() {
        return directInvokeRequest;
    }

    public void setDirectInvokeRequest(DirectInvokeRequest directInvokeRequest) {
        this.directInvokeRequest = directInvokeRequest;
    }

    public boolean isExecutionOnDemand() {
        return executionOnDemand;
    }

    public void setExecutionOnDemand(boolean executionOnDemand) {
        this.executionOnDemand = executionOnDemand;
    }

    public JunitOptions getJunitOption() {
        return junitOption;
    }

    public void setJunitOption(JunitOptions junitOption) {
        this.junitOption = junitOption;
    }

    public String getTestCasePath() {
        return testCasePath;
    }

    public void setTestCasePath(String testCasePath) {
        this.testCasePath = testCasePath;
    }

    public boolean isPreview() {
        return isPreview;
    }

    public void setPreview(boolean preview) {
        isPreview = preview;
    }

    public boolean isMockCalls() {
        return mockCalls;
    }

    public void setMockCalls(boolean mockCalls) {
        this.mockCalls = mockCalls;
    }

    public boolean isUseMockitoAnnotation() {
        return useMockitoAnnotation;
    }

    public void setUseMockitoAnnotation(boolean useMockitoAnnotation) {
        this.useMockitoAnnotation = useMockitoAnnotation;
    }

    public TestFrameworkOptions getTestFrameworkOption() {
        return testFrameworkOption;
    }

    public void setTestFrameworkOption(TestFrameworkOptions testFrameworkOption) {
        this.testFrameworkOption = testFrameworkOption;
    }

    public MockFrameworkOptions getMockFrameworkOption() {
        return mockFrameworkOption;
    }

    public void setMockFrameworkOption(MockFrameworkOptions mockFrameworkOption) {
        this.mockFrameworkOption = mockFrameworkOption;
    }

    public JSONFrameworkOptions getJsonFrameworkOption() {
        return jsonFrameworkOption;
    }

    public void setJsonFrameworkOption(JSONFrameworkOptions jsonFrameworkOption) {
        this.jsonFrameworkOption = jsonFrameworkOption;
    }

    public StoreOptions getStoreOption() {
        return storeOption;
    }

    public void setStoreOption(StoreOptions storeOption) {
        this.storeOption = storeOption;
    }
}
