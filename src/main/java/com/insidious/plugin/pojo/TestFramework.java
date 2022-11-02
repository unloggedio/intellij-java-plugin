package com.insidious.plugin.pojo;

import com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory;
import com.squareup.javapoet.ClassName;

public enum TestFramework {
    JUNIT4(
            MethodCallExpressionFactory.makeParameter("Assert", "org.junit.Assert"),
            "assertEquals",
            ClassName.bestGuess("org.junit.Before"),
            ClassName.bestGuess("org.junit.Test")
    ),
    JUNIT5(
            MethodCallExpressionFactory.makeParameter("Assertions", "org.junit.jupiter.api.Assertions"),
            "assertEquals",
            ClassName.bestGuess("org.junit.jupiter.api.BeforeEach"),
            ClassName.bestGuess("org.junit.jupiter.api.Test")
    );

    private final Parameter assertClassParameter;
    private final String assertEqualMethodName;
    private final ClassName beforeAnnotationType;
    private final ClassName testAnnotationType;

    TestFramework(
            Parameter assertClassParameter,
            String assertEqualMethodName,
            ClassName beforeAnnotationType,
            ClassName testAnnotationType
    ) {
        this.assertClassParameter = assertClassParameter;
        this.assertEqualMethodName = assertEqualMethodName;
        this.beforeAnnotationType = beforeAnnotationType;
        this.testAnnotationType = testAnnotationType;
    }

    public Parameter AssertClassParameter() {
        return assertClassParameter;
    }

    public String AssertEqualMethod() {
        return assertEqualMethodName;
    }

    public ClassName getBeforeAnnotationType() {
        return beforeAnnotationType;
    }

    public ClassName getTestAnnotationType() {
        return testAnnotationType;
    }

}
