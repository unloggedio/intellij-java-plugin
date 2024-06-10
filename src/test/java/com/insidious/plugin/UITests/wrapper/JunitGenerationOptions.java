package com.insidious.plugin.UITests.wrapper;

public class JunitGenerationOptions {
    private String path;
    private String name;
    private Boolean mockDownstreamCalls = true;
    private Boolean useMockitoAnnotation = false;
    private TestFrameworkOptions frameworkOptions;
    private MockFrameworkOptions mockFrameworkOption;
    private JSONFrameworkOptions jsonFrameworkOption;
    private StoreOptions storeOption;

    public JunitGenerationOptions(String path, String name, Boolean mockDownstreamCalls, Boolean useMockitoAnnotation, TestFrameworkOptions frameworkOptions, MockFrameworkOptions mockFrameworkOption, JSONFrameworkOptions jsonFrameworkOption, StoreOptions storeOption) {
        this.path = path;
        this.name = name;
        this.mockDownstreamCalls = mockDownstreamCalls;
        this.useMockitoAnnotation = useMockitoAnnotation;
        this.frameworkOptions = frameworkOptions;
        this.mockFrameworkOption = mockFrameworkOption;
        this.jsonFrameworkOption = jsonFrameworkOption;
        this.storeOption = storeOption;
    }

    public static JunitGenerationOptions defaultOptions() {
        return new JunitGenerationOptions(null, null, null,
                null, null, null,
                null, null);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isMockDownstreamCalls() {
        return mockDownstreamCalls;
    }

    public void setMockDownstreamCalls(boolean mockDownstreamCalls) {
        this.mockDownstreamCalls = mockDownstreamCalls;
    }

    public boolean isUseMockitoAnnotation() {
        return useMockitoAnnotation;
    }

    public void setUseMockitoAnnotation(boolean useMockitoAnnotation) {
        this.useMockitoAnnotation = useMockitoAnnotation;
    }

    public TestFrameworkOptions getFrameworkOptions() {
        return frameworkOptions;
    }

    public void setFrameworkOptions(TestFrameworkOptions frameworkOptions) {
        this.frameworkOptions = frameworkOptions;
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
