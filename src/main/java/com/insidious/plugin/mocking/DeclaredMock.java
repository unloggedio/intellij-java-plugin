package com.insidious.plugin.mocking;

import java.util.List;

public class DeclaredMock {

    private String name;
    private String fieldTypeName;
    private String fieldName;
    private String methodName;
    private List<ParameterMatcher> whenParameter;
    private ReturnValue returnParameter;
    private MethodExitType methodExitType;

    public DeclaredMock() {
    }

    public DeclaredMock(String name, String fieldTypeName, String fieldName, String methodName,
                        List<ParameterMatcher> whenParameter,
                        ReturnValue returnParameter, MethodExitType methodExitType) {
        this.name = name;
        this.fieldTypeName = fieldTypeName;
        this.fieldName = fieldName;
        this.methodName = methodName;
        this.whenParameter = whenParameter;
        this.returnParameter = returnParameter;
        this.methodExitType = methodExitType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MethodExitType getMethodExitType() {
        return methodExitType;
    }

    public String getFieldTypeName() {
        return fieldTypeName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<ParameterMatcher> getWhenParameter() {
        return whenParameter;
    }

    public ReturnValue getReturnParameter() {
        return returnParameter;
    }

    public MethodExitType getReturnType() {
        return methodExitType;
    }
}
