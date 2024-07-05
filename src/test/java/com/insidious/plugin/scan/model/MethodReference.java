package com.insidious.plugin.scan.model;

public class MethodReference {
    private String methodName;
    private String containingClass;

    public MethodReference(String methodName, String containingClass) {
        this.methodName = methodName;
        this.containingClass = containingClass;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getContainingClass() {
        return containingClass;
    }

    public void setContainingClass(String containingClass) {
        this.containingClass = containingClass;
    }

    @Override
    public String toString() {
        return "MethodReference{" +
                "methodName='" + methodName + '\'' +
                ", containingClass='" + containingClass + '\'' +
                '}';
    }
}
