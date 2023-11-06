package com.insidious.plugin.agent;

import java.util.HashSet;

public class AgentCommandResponse<T> {
    private T methodReturnValue;
    private ResponseType responseType;
    private String responseClassName;
    private String message;
    private String targetMethodName;
    private String targetClassName;
    private String targetMethodSignature;
    private long timestamp;
    private HashSet<String> enabledMockId;

    public HashSet<String> getEnabledMockId() {
        return this.enabledMockId;
    }

    public void setEnabledMockId(HashSet<String> enabledMockId) {
        this.enabledMockId = enabledMockId;
    }


    public AgentCommandResponse(ResponseType responseType) {
        this.responseType = responseType;
    }

    public AgentCommandResponse() {
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getTargetMethodName() {
        return targetMethodName;
    }

    public void setTargetMethodName(String targetMethodName) {
        this.targetMethodName = targetMethodName;
    }

    public String getTargetClassName() {
        return targetClassName;
    }

    public void setTargetClassName(String targetClassName) {
        this.targetClassName = targetClassName;
    }

    public String getTargetMethodSignature() {
        return targetMethodSignature;
    }

    public void setTargetMethodSignature(String targetMethodSignature) {
        this.targetMethodSignature = targetMethodSignature;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ResponseType getResponseType() {
        return responseType;
    }

    public void setResponseType(ResponseType responseType) {
        this.responseType = responseType;
    }

    public String getResponseClassName() {
        return responseClassName;
    }

    public void setResponseClassName(String responseClassName) {
        this.responseClassName = responseClassName;
    }

    public T getMethodReturnValue() {
        return methodReturnValue;
    }

    public void setMethodReturnValue(T methodReturnValue) {
        this.methodReturnValue = methodReturnValue;
    }

    @Override
    public String toString() {
        return "AgentCommandResponse{" +
                "methodReturnValue=" + methodReturnValue +
                ", message='" + message + '\'' +
                '}';
    }

}
