package com.insidious.plugin.pojo.frameworks;

import com.insidious.plugin.pojo.Parameter;
import com.squareup.javapoet.ClassName;

import static com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory.makeParameter;

public enum MockFramework {
    Mockito(ClassName.bestGuess("org.mockito.Mockito"), makeParameter("Mockito", "org.mockito.Mockito", -99885));

    private final ClassName className;
    private final Parameter mockClassParameter;

    MockFramework(ClassName className, Parameter parameter) {
        this.className = className;
        this.mockClassParameter = parameter;
    }

    public Parameter getMockClassParameter() {
        return mockClassParameter;
    }

    public ClassName getClassName() {
        return className;
    }
}
