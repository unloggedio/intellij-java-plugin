package com.insidious.plugin.coverage;

public class ClassCoverageData {
    String className;
    private int totalMethodCount;
    private int coveredMethodCount;
    private int totalLineCount;
    private int coveredLineCount;
    private int totalBranchCount;
    private int coveredBranchCount;

    public ClassCoverageData(String className,
                             int totalMethodCount, int coveredMethodCount,
                             int totalLineCount, int coveredLineCount,
                             int totalBranchCount, int coveredBranchCount) {
        this.className = className;
        this.totalMethodCount = totalMethodCount;
        this.coveredMethodCount = coveredMethodCount;
        this.totalLineCount = totalLineCount;
        this.coveredLineCount = coveredLineCount;
        this.totalBranchCount = totalBranchCount;
        this.coveredBranchCount = coveredBranchCount;
    }

    public String getClassName() {
        return className;
    }

    public int getTotalMethodCount() {
        return totalMethodCount;
    }

    public int getCoveredMethodCount() {
        return coveredMethodCount;
    }

    public int getTotalLineCount() {
        return totalLineCount;
    }

    public int getCoveredLineCount() {
        return coveredLineCount;
    }

    public int getTotalBranchCount() {
        return totalBranchCount;
    }

    public int getCoveredBranchCount() {
        return coveredBranchCount;
    }
}
