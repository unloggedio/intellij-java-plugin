package com.insidious.plugin.mocking;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class DeclaredMock implements Comparable<DeclaredMock> {


    private String id = UUID.randomUUID().toString();
    private String name;
    private String fieldTypeName;
    private String sourceClassName;
    private String fieldName;
    private String methodName;
    private List<ParameterMatcher> whenParameter;
    private List<ThenParameter> thenParameter;
    private String methodHashKey;

    public DeclaredMock(DeclaredMock declaredMock) {
        this.id = declaredMock.id;
        this.name = declaredMock.name;
        this.sourceClassName = declaredMock.sourceClassName;
        this.fieldTypeName = declaredMock.fieldTypeName;
        this.fieldName = declaredMock.fieldName;
        this.methodName = declaredMock.methodName;
        this.methodHashKey = declaredMock.methodHashKey;
        this.whenParameter = declaredMock.whenParameter.stream().map(ParameterMatcher::new)
                .collect(Collectors.toList());
        this.thenParameter = declaredMock.thenParameter.stream().map(ThenParameter::new).collect(Collectors.toList());

    }

    public DeclaredMock(String name, String fieldTypeName, String sourceClassName,
                        String fieldName, String methodName, String methodHashKey,
                        List<ParameterMatcher> whenParameterLists,
                        List<ThenParameter> thenParameterList
    ) {
        this.name = name;
        this.fieldTypeName = fieldTypeName;
        this.sourceClassName = sourceClassName;
        this.fieldName = fieldName;
        this.methodName = methodName;
        this.whenParameter = whenParameterLists;
        this.thenParameter = thenParameterList;
        this.methodHashKey = methodHashKey;
    }

    public DeclaredMock() {
    }


    public String getSourceClassName() {
        return sourceClassName;
    }

    public void setSourceClassName(String sourceClassName) {
        this.sourceClassName = sourceClassName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeclaredMock that = (DeclaredMock) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
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

    @Override
    public int compareTo(DeclaredMock o) {
        return this.id.compareTo(o.id);
    }

    public String getMethodHashKey() {
        return methodHashKey;
    }

    public void setMethodHashKey(String methodHashKey) {
        this.methodHashKey = methodHashKey;
    }
}
