package com.insidious.plugin.pojo;

import com.insidious.plugin.factory.testcase.TestGenerationState;
import com.squareup.javapoet.TypeSpec;

public class TestCaseUnit {


    private final String code;
    private final String packageName;
    private final String className;
    private final TestGenerationState testGenerationState;
    private TypeSpec testClassSpec;
    private final String testMethodName;

    public TypeSpec getTestClassSpec() {
        return testClassSpec;
    }

    public TestCaseUnit(String code, String packageName,
                        String className,
                        String testMethodName,
                        TestGenerationState testGenerationState,
                        TypeSpec testClassSpec) {
        this.code = code;
        this.packageName = packageName;
        this.className = className;
        this.testMethodName = testMethodName;
        this.testGenerationState = testGenerationState;
        this.testClassSpec = testClassSpec;
    }

    public String getTestMethodName() {
        return testMethodName;
    }

    public TestGenerationState getTestGenerationState() {
        return testGenerationState;
    }

    @Override
    public String toString() {
        return code;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getCode() {
        return code;
    }

    public String getClassName() {
        return className;
    }
}
