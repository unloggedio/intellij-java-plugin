package com.insidious.plugin.extension.model;


public class StringInfo {


    String executionSessionId;

    private String content;
    private Long stringId;

    public StringInfo() {
    }

    public StringInfo(Long stringId, String content) {

        this.stringId = stringId;
        this.content = content;
    }

    public String getExecutionSessionId() {
        return executionSessionId;
    }

    public void setExecutionSessionId(String executionSessionId) {
        this.executionSessionId = executionSessionId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getStringId() {
        return stringId;
    }

    public void setStringId(Long stringId) {
        this.stringId = stringId;
    }

    @Override
    public String toString() {
        return content;
    }
}
