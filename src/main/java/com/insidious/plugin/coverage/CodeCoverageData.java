package com.insidious.plugin.coverage;

import java.util.List;

public class CodeCoverageData {
    List<PackageCoverageData> packageCoverageDataList;

    public CodeCoverageData(List<PackageCoverageData> packageCoverageDataList) {
        this.packageCoverageDataList = packageCoverageDataList;
    }

    public List<PackageCoverageData> getPackageCoverageDataList() {
        return packageCoverageDataList;
    }
}
