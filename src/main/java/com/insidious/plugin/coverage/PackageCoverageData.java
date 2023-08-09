package com.insidious.plugin.coverage;

import java.util.List;

public class PackageCoverageData {
    String packageName;
    List<ClassCoverageData> classCoverageDataList;

    public String getPackageName() {
        return packageName;
    }

    public List<ClassCoverageData> getClassCoverageDataList() {
        return classCoverageDataList;
    }

    public PackageCoverageData(String packageName, List<ClassCoverageData> classCoverageDataList) {
        this.packageName = packageName;
        this.classCoverageDataList = classCoverageDataList;
    }
}
