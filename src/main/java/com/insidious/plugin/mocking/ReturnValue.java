package com.insidious.plugin.mocking;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReturnValue {
    public ReturnValue() {
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReturnValue that = (ReturnValue) o;
        return Objects.equals(declaredMocks, that.declaredMocks) && value.equals(
                that.value) && returnValueType == that.returnValueType && className.equals(that.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(declaredMocks, value, returnValueType, className);
    }

    public void setReturnValueType(ReturnValueType returnValueType) {
        this.returnValueType = returnValueType;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    private final List<DeclaredMock> declaredMocks = new ArrayList<>();
    private String value;
    private ReturnValueType returnValueType;
    private String className;

    public ReturnValue(String value, String returnValueClassName, ReturnValueType returnValueType) {
        this.value = value;
        this.className = returnValueClassName;
        this.returnValueType = returnValueType;
    }


    public ReturnValue(ReturnValue returnParameter) {
        this.value = returnParameter.value;
        this.className = returnParameter.className;
        this.returnValueType = returnParameter.returnValueType;
        this.declaredMocks.addAll(returnParameter.declaredMocks
                .stream().map(DeclaredMock::new).collect(Collectors.toList()));

    }

    public String getClassName() {
        return className;
    }

    public String getValue() {
        return value;
    }

    public ReturnValueType getReturnValueType() {
        return returnValueType;
    }

    public void addDeclaredMock(DeclaredMock mockDefinition) {
        declaredMocks.add(mockDefinition);
    }

    public List<DeclaredMock> getDeclaredMocks() {
        return declaredMocks;
    }

    @Override
    public String toString() {
        return "ReturnValue{" +
                "value='" + value + '\'' +
                ", returnValueType=" + returnValueType +
                ", className='" + className + '\'' +
                '}';
    }
}
