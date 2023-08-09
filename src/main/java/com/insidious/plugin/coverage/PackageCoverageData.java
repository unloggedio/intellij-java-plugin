package com.insidious.plugin.coverage;

import java.util.List;

public class PackageCoverageData {
    private final int totalClassCount;
    private final long coveredClassCount;
    private final String packageName;
    private final List<ClassCoverageData> classCoverageDataList;

    public PackageCoverageData(String packageName, List<ClassCoverageData> classCoverageDataList) {
        this.packageName = packageName;
        this.classCoverageDataList = classCoverageDataList;
        totalClassCount = classCoverageDataList.size();
        coveredClassCount = classCoverageDataList.stream().filter(e -> e.getCoveredLineCount() > 0).count();
    }

    public String getPackageName() {
        return packageName;
    }

    public List<ClassCoverageData> getClassCoverageDataList() {
        return classCoverageDataList;
    }

    public int getTotalClassCount() {
        return totalClassCount;
    }

    public long getCoveredClassCount() {
        return coveredClassCount;
    }
}
