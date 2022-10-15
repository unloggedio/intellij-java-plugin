package com.insidious.plugin.client;

import org.jetbrains.annotations.NotNull;

public class TestCandidateMethodAggregate implements Comparable<TestCandidateMethodAggregate> {
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

    @Override
    public int compareTo(@NotNull TestCandidateMethodAggregate o) {
        return this.methodName.compareTo(o.getMethodName());
    }
}
