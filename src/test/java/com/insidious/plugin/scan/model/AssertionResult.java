package com.insidious.plugin.scan.model;

public class AssertionResult {
    private String expectedValue;
    private String scannedValue;
    private boolean passing;

    public String getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    public String getScannedValue() {
        return scannedValue;
    }

    public void setScannedValue(String scannedValue) {
        this.scannedValue = scannedValue;
    }

    public boolean isPassing() {
        return passing;
    }

    public void setPassing(boolean passing) {
        this.passing = passing;
    }

    public AssertionResult(String expectedValue, String scannedValue, boolean passing) {
        this.expectedValue = expectedValue;
        this.scannedValue = scannedValue;
        this.passing = passing;
    }
}
