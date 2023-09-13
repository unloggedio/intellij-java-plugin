package com.insidious.plugin.mocking;

import java.util.List;
import java.util.stream.Collectors;

public class DeclaredMock {

    private String name;
    private String fieldTypeName;
    private String fieldName;
    private String methodName;
    private List<ParameterMatcher> whenParameter;
    private List<ThenParameter> thenParameter;

    public DeclaredMock(DeclaredMock declaredMock) {
        this.name = declaredMock.name;
        this.fieldTypeName = declaredMock.fieldTypeName;
        this.fieldName = declaredMock.fieldName;
        this.methodName = declaredMock.methodName;
        this.whenParameter = declaredMock.whenParameter.stream()
                .map(ParameterMatcher::new).collect(Collectors.toList());
        this.thenParameter = declaredMock.thenParameter
                .stream().map(ThenParameter::new).collect(Collectors.toList());

    }

    public DeclaredMock(String name, String fieldTypeName, String fieldName, String methodName,
                        List<ParameterMatcher> whenParameterLists,
                        List<ThenParameter> thenParameterList
    ) {
        this.name = name;
        this.fieldTypeName = fieldTypeName;
        this.fieldName = fieldName;
        this.methodName = methodName;
        this.whenParameter = whenParameterLists;
        this.thenParameter = thenParameterList;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getFieldTypeName() {
        return fieldTypeName;
    }

    public void setFieldTypeName(String fieldTypeName) {
        this.fieldTypeName = fieldTypeName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public List<ParameterMatcher> getWhenParameter() {
        return whenParameter;
    }

    public void setWhenParameter(List<ParameterMatcher> whenParameter) {
        this.whenParameter = whenParameter;
    }

    public List<ThenParameter> getThenParameter() {
        return thenParameter;
    }

    public void setThenParameter(List<ThenParameter> thenParameter) {
        this.thenParameter = thenParameter;
    }
}
