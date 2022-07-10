package com.insidious.plugin.factory;

import com.squareup.javapoet.MethodSpec;

public class TestMethodScript {
    private final MethodSpec methodSpec;
    private final String testSignature;

    public TestMethodScript(MethodSpec methodSpec, String testSignature) {
        this.methodSpec = methodSpec;
        this.testSignature = testSignature;
    }

    public MethodSpec getMethodSpec() {
        return methodSpec;
    }

    public String getTestSignature() {
        return testSignature;
    }
}
