package com.insidious.plugin.UITests.wrapper;

public class JunitGenerationRequest {

    private DirectInvokeRequest directInvokeRequest;
    private boolean executeOnDemand;
    private JunitGenerationMethod junitGenerationMethod;
    private String testBasePath;
    private boolean isPreview = false;
    private JunitGenerationOptions junitGenerationOptions;
    private FilterOptions filterOptions;

    private JunitGenerationRequest(DirectInvokeRequest directInvokeRequest, JunitGenerationMethod junitGenerationMethod,
                                   String testBasePath, JunitGenerationOptions options, boolean executeOnDemand) {
        this.directInvokeRequest = directInvokeRequest;
        this.junitGenerationMethod = junitGenerationMethod;
        this.testBasePath = testBasePath;
        this.junitGenerationOptions = options;
        this.isPreview = false;
        this.executeOnDemand = executeOnDemand;
    }

    public static JunitGenerationRequest fromDirectInvokeRequest(DirectInvokeRequest directInvokeRequest,
                                                                 GitProjectInfo projectInfo, boolean executeOnDemand, JunitGenerationMethod creationMethod,
                                                                 JunitGenerationOptions junitGenerationOptions) {
        return new JunitGenerationRequest(directInvokeRequest,
                creationMethod, projectInfo.getTestBasePath(), junitGenerationOptions, executeOnDemand);
    }

    public DirectInvokeRequest getDirectInvokeRequest() {
        return directInvokeRequest;
    }

    public void setDirectInvokeRequest(DirectInvokeRequest directInvokeRequest) {
        this.directInvokeRequest = directInvokeRequest;
    }

    public boolean isExecuteOnDemand() {
        return executeOnDemand;
    }

    public void setExecuteOnDemand(boolean executeOnDemand) {
        this.executeOnDemand = executeOnDemand;
    }

    public JunitGenerationMethod getJunitGenerationMethod() {
        return junitGenerationMethod;
    }

    public void setJunitGenerationMethod(JunitGenerationMethod junitGenerationM) {
        this.junitGenerationMethod = junitGenerationM;
    }

    public String getTestBasePath() {
        return testBasePath;
    }

    public void setTestBasePath(String testBasePath) {
        this.testBasePath = testBasePath;
    }

    public boolean isPreview() {
        return isPreview;
    }

    public void setPreview(boolean preview) {
        isPreview = preview;
    }

    public JunitGenerationOptions getJunitGenerationOptions() {
        return junitGenerationOptions;
    }

    public void setJunitGenerationOptions(JunitGenerationOptions junitGenerationOptions) {
        this.junitGenerationOptions = junitGenerationOptions;
    }

    public FilterOptions getFilterOptions() {
        return filterOptions;
    }

    public void setFilterOptions(FilterOptions filterOptions) {
        this.filterOptions = filterOptions;
    }
}
