package com.insidious.plugin.pojo.atomic;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;

public class MethodUnderTest {
    String name;
    String signature;
    String className;
    int methodHash;


    public MethodUnderTest(String name, String signature, int methodHash, String className) {
        this.name = name;
        this.signature = signature;
        this.className = className;
        this.methodHash = methodHash;
    }

    public MethodUnderTest() {
    }

    public static MethodUnderTest fromMethodAdapter(MethodAdapter methodElement) {
        final int methodHash = methodElement.getText().hashCode();
        final String methodName = methodElement.getName();
        final String methodJVMSignature = methodElement.getJVMSignature();
        final String classQualifiedName = methodElement.getContainingClass().getQualifiedName();
        return new MethodUnderTest(methodName, methodJVMSignature, methodHash, classQualifiedName);
    }

    public static MethodUnderTest fromTestCandidateMetadata(TestCandidateMetadata testCandidateMetadata) {
        return new MethodUnderTest(
                testCandidateMetadata.getMainMethod().getMethodName(), null, 0,
                testCandidateMetadata.getFullyQualifiedClassname());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getMethodHash() {
        return methodHash;
    }

    public void setMethodHash(int methodHash) {
        this.methodHash = methodHash;
    }

    @Override
    public String toString() {
        return "Method{" +
                className + "." + name + "(" + signature + ") " + methodHash +
                '}';
    }

    @JsonIgnore
    public String getMethodKey() {
        return name + "#" + signature;
    }

}
