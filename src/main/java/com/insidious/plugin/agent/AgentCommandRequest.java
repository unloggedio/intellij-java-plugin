package com.insidious.plugin.agent;

import java.util.List;

public class AgentCommandRequest {
    private AgentCommand command;
    private String className;
    private String methodName;
    private String methodSignature;
    private List<String> methodParameters;

    public AgentCommand getCommand() {
        return command;
    }

    public void setCommand(AgentCommand command) {
        this.command = command;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }

    public List<String> getMethodParameters() {
        return methodParameters;
    }

    @Override
    public String toString() {
        return "AgentCommandRequest{" +
                "command=" + command +
                ", className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", methodSignature='" + methodSignature + '\'' +
                ", methodParameters=" + methodParameters +
                '}';
    }

    public void setMethodParameters(List<String> methodParameters) {
        this.methodParameters = methodParameters;
    }
}
