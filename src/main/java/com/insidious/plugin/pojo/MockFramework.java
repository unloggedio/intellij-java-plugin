package com.insidious.plugin.pojo;

import com.squareup.javapoet.ClassName;

import static com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory.makeParameter;

public enum MockFramework {
    Mockito(ClassName.bestGuess("org.mockito.Mockito"), makeParameter("Mockito", "org.mockito.Mockito"));

    private Parameter mockClassParameter;

    MockFramework(ClassName bestGuess, Parameter parameter) {
        this.mockClassParameter = parameter;
    }

    public Parameter getMockClassParameter() {
        return mockClassParameter;
    }

    public void setMockClassParameter(Parameter mockClassParameter) {
        this.mockClassParameter = mockClassParameter;
    }
}
