package com.insidious.plugin.pojo;

import com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory;
import com.squareup.javapoet.ClassName;

public enum TestFramework {
    JUNIT4(
            MethodCallExpressionFactory.makeParameter("Assert", "org.junit.Assert", ConstructorType.SINGLETON),
            "assertEquals",
            ClassName.bestGuess("org.junit.Before")
    ),
    JUNIT5(
            MethodCallExpressionFactory.makeParameter("Assert", "org.junit.Assert", ConstructorType.SINGLETON),
            "assertEquals",
            ClassName.bestGuess("org.junit.Before")
    );

    private final Parameter assertClassParameter;
    private final String assertEqualMethodName;
    private final ClassName beforeAnnotationType;

    TestFramework(
            Parameter assertClassParameter,
            String assertEqualMethodName,
            ClassName beforeAnnotationType
    ) {
        this.assertClassParameter = assertClassParameter;
        this.assertEqualMethodName = assertEqualMethodName;
        this.beforeAnnotationType = beforeAnnotationType;
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

}
