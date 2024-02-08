package com.insidious.plugin.autoexecutor.testutils.entity;

public class TestResultSummary {
    private int numberOfCases;
    private int passingCasesCount;
    private int failingCasesCount;

    public TestResultSummary(int numberOfCases, int passingCasesCount, int failingCasesCount) {
        this.numberOfCases = numberOfCases;
        this.passingCasesCount = passingCasesCount;
        this.failingCasesCount = failingCasesCount;
    }

    public int getNumberOfCases() {
        return numberOfCases;
    }

    public int getPassingCasesCount() {
        return passingCasesCount;
    }

    public int getFailingCasesCount() {
        return failingCasesCount;
    }
}
