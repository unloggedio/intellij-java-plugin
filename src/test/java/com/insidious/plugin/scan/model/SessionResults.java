package com.insidious.plugin.scan.model;

public class SessionResults {

    private boolean passing;
    private int totalCaseCount;
    private int passingCount;

    public SessionResults(boolean passing, int totalCaseCount, int passingCount) {
        this.passing = passing;
        this.totalCaseCount = totalCaseCount;
        this.passingCount = passingCount;
    }

    public boolean isPassing() {
        return passing;
    }

    public int getTotalCaseCount() {
        return totalCaseCount;
    }

    public int getPassingCount() {
        return passingCount;
    }

    public String getPassingOutOfTotalCount() {
        return "(" + passingCount + "/" + totalCaseCount + ")";
    }
}
