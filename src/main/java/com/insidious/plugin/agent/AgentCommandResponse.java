package com.insidious.plugin.agent;

public class AgentCommandResponse {
    private String methodReturnValue;
    private ResponseType responseType;
    private String responseClassName;
    private String message;

    public AgentCommandResponse(ResponseType responseType) {
        this.responseType = responseType;
    }

    public AgentCommandResponse() {
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

    public String getMethodReturnValue() {
        return methodReturnValue;
    }

    public void setMethodReturnValue(String methodReturnValue) {
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
