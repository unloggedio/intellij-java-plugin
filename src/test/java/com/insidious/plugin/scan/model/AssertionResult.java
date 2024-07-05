package com.insidious.plugin.scan.model;

public class AssertionResult {
    private String expectedValue;
    private String scannedValue;
    private boolean passing;
    private Long expectedCount;
    private Long actualCount;

    public Long getExpectedCount() {
        return expectedCount;
    }

    public void setExpectedCount(Long expectedCount) {
        this.expectedCount = expectedCount;
    }

    public Long getActualCount() {
        return actualCount;
    }

    public void setActualCount(Long actualCount) {
        this.actualCount = actualCount;
    }

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

    public AssertionResult(String expectedValue, String scannedValue, boolean passing, Long expectedCount, Long actualCount) {
        this.expectedValue = expectedValue;
        this.scannedValue = scannedValue;
        this.passing = passing;
        this.expectedCount = expectedCount;
        this.actualCount = actualCount;
    }
}
