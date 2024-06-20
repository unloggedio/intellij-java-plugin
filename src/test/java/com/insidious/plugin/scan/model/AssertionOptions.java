package com.insidious.plugin.scan.model;

public class AssertionOptions {
    private String expectedValue;
    private Long count;

    public AssertionOptions() {
    }

    public AssertionOptions(String expectedValue, Long count) {
        this.expectedValue = expectedValue;
        this.count = count;
    }

    public AssertionOptions(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    public AssertionOptions(Long count) {
        this.count = count;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
