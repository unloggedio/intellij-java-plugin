package com.insidious.plugin.pojo.atomic;

public class ClassUnderTest {
    private final String qualifiedClassName;

    public ClassUnderTest(String qualifiedClassName) {
        this.qualifiedClassName = qualifiedClassName;
    }

    public String getQualifiedClassName() {
        return qualifiedClassName;
    }
}
