package com.insidious.plugin.client;

public class TestCandidateMethodAggregate {
    private final String className;
    String methodName;
    Integer count;

    public TestCandidateMethodAggregate(String className, String methodName, Integer count) {
        this.className = className;
        this.methodName = methodName;
        this.count = count;
    }

    public String getMethodName() {
        return methodName;
    }

    public Integer getCount() {
        return count;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public String toString() {
        return methodName + " - [" + count + " items]";
    }
}
