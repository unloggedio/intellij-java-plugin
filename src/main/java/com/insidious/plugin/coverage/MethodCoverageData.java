package com.insidious.plugin.coverage;

public class MethodCoverageData {
    private final int totalLineCount;
    private final int totalBranchCount;
    private final String methodName;
    private final String methodSignature;
    private int coveredLineCount;
    private int coveredBranchCount;

    public MethodCoverageData(String methodName, String methodSignature, int totalLineCount, int coveredLineCount, int totalBranchCount, int coveredBranchCount) {
        this.methodName = methodName;
        this.methodSignature = methodSignature;
        this.totalLineCount = totalLineCount;
        this.coveredLineCount = coveredLineCount;
        this.totalBranchCount = totalBranchCount;
        this.coveredBranchCount = coveredBranchCount;
    }

    public MethodCoverageData(String methodName, String methodSignature,
                              int totalLineCount,
                              int totalBranchCount) {
        this.methodName = methodName;
        this.methodSignature = methodSignature;
        this.totalLineCount = totalLineCount;
        this.totalBranchCount = totalBranchCount;
    }

    public int getTotalLineCount() {
        return totalLineCount;
    }

    public int getCoveredLineCount() {
        return coveredLineCount;
    }

    public void setCoveredLineCount(int coveredLineCount) {
        this.coveredLineCount = coveredLineCount;
    }

    public int getTotalBranchCount() {
        return totalBranchCount;
    }

    public int getCoveredBranchCount() {
        return coveredBranchCount;
    }

    public void setCoveredBranchCount(int coveredBranchCount) {
        this.coveredBranchCount = coveredBranchCount;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodSignature() {
        return methodSignature;
    }
}
