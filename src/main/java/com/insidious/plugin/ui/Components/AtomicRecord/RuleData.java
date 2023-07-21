package com.insidious.plugin.ui.Components.AtomicRecord;

public class RuleData {
    private String context;
    private String key;
    private String operation;
    private String expectedValue;
    private String status;

    //add type

    public RuleData(String context, String key, String operation, String expectedValue, String status) {
        this.context = context;
        this.key = key;
        this.operation = operation;
        this.expectedValue = expectedValue;
        this.status = status;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "RuleData{" +
                "context='" + context + '\'' +
                ", key='" + key + '\'' +
                ", operation='" + operation + '\'' +
                ", expectedValue='" + expectedValue + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
