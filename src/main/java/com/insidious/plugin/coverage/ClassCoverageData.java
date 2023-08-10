package com.insidious.plugin.coverage;

import java.util.List;

public class ClassCoverageData {
    private final String className;
    private final List<MethodCoverageData> methodCoverageData;
    private final int totalMethodCount;
    private final int coveredMethodCount;
    private final int totalLineCount;
    private final int coveredLineCount;
    private final int totalBranchCount;
    private final int coveredBranchCount;

    public List<MethodCoverageData> getMethodCoverageData() {
        return methodCoverageData;
    }

    public ClassCoverageData(String className, List<MethodCoverageData> methodCoverageData) {
        this.className = className;
        this.methodCoverageData = methodCoverageData;
        totalMethodCount = methodCoverageData.size();
        coveredMethodCount = Math.toIntExact(
                methodCoverageData.stream().filter(e -> e.getCoveredLineCount() > 0).count());
        totalLineCount =
                methodCoverageData.stream().mapToInt(MethodCoverageData::getTotalLineCount).sum();
        coveredLineCount =
                methodCoverageData.stream().mapToInt(MethodCoverageData::getCoveredLineCount).sum();
        totalBranchCount =
                methodCoverageData.stream().mapToInt(MethodCoverageData::getTotalBranchCount).sum();
        coveredBranchCount =
                methodCoverageData.stream().mapToInt(MethodCoverageData::getCoveredBranchCount).sum();
    }

    @Override
    public String toString() {
        return className;
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
