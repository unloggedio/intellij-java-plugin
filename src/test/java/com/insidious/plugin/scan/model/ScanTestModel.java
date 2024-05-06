package com.insidious.plugin.scan.model;

import java.util.Map;

public class ScanTestModel {
    private String sessionFolder;
    private Map<MethodReference, String> assertions;

    public String getSessionFolder() {
        return sessionFolder;
    }

    public void setSessionFolder(String sessionFolder) {
        this.sessionFolder = sessionFolder;
    }

    public Map<MethodReference, String> getAssertions() {
        return assertions;
    }

    public void setAssertions(Map<MethodReference, String> assertions) {
        this.assertions = assertions;
    }

    public ScanTestModel(String sessionFolder, Map<MethodReference, String> assertions) {
        this.sessionFolder = sessionFolder;
        this.assertions = assertions;
    }
}
