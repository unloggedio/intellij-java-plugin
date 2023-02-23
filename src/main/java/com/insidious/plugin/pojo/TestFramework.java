package com.insidious.plugin.pojo;

import com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory;
import com.squareup.javapoet.ClassName;

public enum TestFramework {
    JUnit4(
            MethodCallExpressionFactory.makeParameter("Assert", "org.junit.Assert"),
            "assertEquals",
            "assertArrayEquals",
            "assertFalse",
            ClassName.bestGuess("org.junit.Before"),
            ClassName.bestGuess("org.junit.After"),
            ClassName.bestGuess("org.junit.Test")
    ),
    JUnit5(
            MethodCallExpressionFactory.makeParameter("Assertions", "org.junit.jupiter.api.Assertions"),
            "assertEquals",
            "assertArrayEquals",
            "assertFalse",
            ClassName.bestGuess("org.junit.jupiter.api.BeforeEach"),
            ClassName.bestGuess("org.junit.jupiter.api.AfterEach"),
            ClassName.bestGuess("org.junit.jupiter.api.Test")
    );

    private final Parameter assertClassParameter;
    private final String assertEqualMethodName;

    private final String assertArrayEqualsMethodName;
    private final String assertFalseMethodName;
    private final ClassName beforeAnnotationType;
    private final ClassName afterAnnotationType;
    private final ClassName testAnnotationType;

    TestFramework(
            Parameter assertClassParameter,
            String assertEqualMethodName,
            String assertArrayEqualsMethodName,
            String assertFalseMethodName,
            ClassName beforeAnnotationType,
            ClassName afterAnnotationType,
            ClassName testAnnotationType
    ) {
        this.assertClassParameter = assertClassParameter;
        this.assertEqualMethodName = assertEqualMethodName;
        this.assertArrayEqualsMethodName = assertArrayEqualsMethodName;
        this.assertFalseMethodName = assertFalseMethodName;
        this.beforeAnnotationType = beforeAnnotationType;
        this.afterAnnotationType = afterAnnotationType;
        this.testAnnotationType = testAnnotationType;
    }

    public Parameter AssertClassParameter() {
        return assertClassParameter;
    }

    public String AssertEqualMethod() {
        return assertEqualMethodName;
    }

    public String AssertArrayEqualsMethod() {
        return assertArrayEqualsMethodName;
    }

    public String AssertFalseMethod() {
        return assertFalseMethodName;
    }

    public ClassName getBeforeAnnotationType() {
        return beforeAnnotationType;
    }

    public ClassName getAfterAnnotationType() {
        return afterAnnotationType;
    }

    public ClassName getTestAnnotationType() {
        return testAnnotationType;
    }

}
