package com.insidious.plugin.scan.model;

import java.util.Map;

public class ScanTestResult {
    private String sessionFolder;
    private Map<MethodReference, AssertionResult> assertionResults;

    public String getSessionFolder() {
        return sessionFolder;
    }

    public void setSessionFolder(String sessionFolder) {
        this.sessionFolder = sessionFolder;
    }

    public Map<MethodReference, AssertionResult> getAssertionResults() {
        return assertionResults;
    }

    public void setAssertionResults(Map<MethodReference, AssertionResult> assertionResults) {
        this.assertionResults = assertionResults;
    }

    public ScanTestResult(String sessionFolder, Map<MethodReference, AssertionResult> assertionResults) {
        this.sessionFolder = sessionFolder;
        this.assertionResults = assertionResults;
    }
}
