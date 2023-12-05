package com.insidious.plugin.agent;

import com.insidious.plugin.auth.RequestAuthentication;
import com.insidious.plugin.mocking.DeclaredMock;

import java.util.List;

public class AgentCommandRequest {
    RequestAuthentication requestAuthentication;
    private AgentCommand command;
    private String className;
    //    private List<String> alternateClassNames;
    private String methodName;
    private String methodSignature;
    private List<String> methodParameters;
    private AgentCommandRequestType requestType;
    private List<String> parameterTypes;
    private List<DeclaredMock> declaredMocks;

    public RequestAuthentication getRequestAuthentication() {
        return requestAuthentication;
    }

    public void setRequestAuthentication(RequestAuthentication requestAuthentication) {
        this.requestAuthentication = requestAuthentication;
    }

    public List<DeclaredMock> getDeclaredMocks() {
        return this.declaredMocks;
    }

    public void setDeclaredMocks(List<DeclaredMock> declaredMocks) {
        this.declaredMocks = declaredMocks;
    }


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

    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(List<String> parameterTypes) {
        this.parameterTypes = parameterTypes;
    }
}
