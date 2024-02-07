package com.insidious.plugin.auto.entity;

import com.insidious.plugin.agent.AgentCommandRequest;
import com.insidious.plugin.agent.AgentCommandResponse;

public class TestUnit {
    private String classname;
    private String methodName;
    private String methodSign;
    private String input;
    private String assertionType;
    private String referenceValue;
    private String refResponseType;
    private AgentCommandRequest sentRequest;
    private AgentCommandResponse<String> response;

    public TestUnit(String classname, String methodName, String methodSign, String input, String assertionType, String referenceValue, String refResponseType, AgentCommandRequest sentRequest, AgentCommandResponse response) {
        this.classname = classname;
        this.methodName = methodName;
        this.methodSign = methodSign;
        this.input = input;
        this.assertionType = assertionType;
        this.referenceValue = referenceValue;
        this.sentRequest = sentRequest;
        this.response = response;
        this.refResponseType = refResponseType;
    }

    public String getClassname() {
        return classname;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodSign() {
        return methodSign;
    }

    public String getInput() {
        return input;
    }

    public String getAssertionType() {
        return assertionType;
    }

    public String getReferenceValue() {
        return referenceValue;
    }

    public AgentCommandRequest getSentRequest() {
        return sentRequest;
    }

    public AgentCommandResponse getResponse() {
        return response;
    }

    public String getRefResponseType() {
        return refResponseType;
    }
}
