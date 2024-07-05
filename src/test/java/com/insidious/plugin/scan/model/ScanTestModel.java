package com.insidious.plugin.scan.model;

import java.util.Map;

public class ScanTestModel {
    private String sessionFolder;
    private Map<MethodReference, AssertionOptions> assertions;

    public String getSessionFolder() {
        return sessionFolder;
    }

    public void setSessionFolder(String sessionFolder) {
        this.sessionFolder = sessionFolder;
    }

    public Map<MethodReference, AssertionOptions> getAssertions() {
        return assertions;
    }

    public void setAssertions(Map<MethodReference, AssertionOptions> assertions) {
        this.assertions = assertions;
    }

    public ScanTestModel(String sessionFolder, Map<MethodReference, AssertionOptions> assertions) {
        this.sessionFolder = sessionFolder;
        this.assertions = assertions;
    }
}
