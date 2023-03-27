package com.insidious.plugin.agent;

public class AgentCommandResponse {
    private Object methodReturnValue;
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setMethodReturnValue(Object methodReturnValue) {
        this.methodReturnValue = methodReturnValue;
    }

    public Object getMethodReturnValue() {
        return methodReturnValue;
    }
}
