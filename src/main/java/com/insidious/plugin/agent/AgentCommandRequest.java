package com.insidious.plugin.agent;

import java.util.List;

public class AgentCommandRequest {
    private AgentCommand command;
    private String className;
//    private List<String> alternateClassNames;
    private String methodName;
    private String methodSignature;
    private List<String> methodParameters;
    private AgentCommandRequestType requestType;

//    public List<String> getAlternateClassNames() {
//        return alternateClassNames;
//    }
//
//    public void setAlternateClassNames(List<String> alternateClassNames) {
//        this.alternateClassNames = alternateClassNames;
//    }

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

    public void setMethodParameters(List<String> methodParameters) {
        this.methodParameters = methodParameters;
    }

    @Override
    public String toString() {
        return "AgentCommandRequest{" +
                "command=" + command + "(" + requestType + ")" +
                ", className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", methodSignature='" + methodSignature + '\'' +
                ", methodParameters=" + methodParameters +
                '}';
    }

    public AgentCommandRequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(AgentCommandRequestType requestType) {
        this.requestType = requestType;
    }
}
