package com.insidious.plugin.autoexecutor;

import com.insidious.plugin.ui.methodscope.DifferenceResult;

import java.util.List;

public class AutoExecutorReportRecord {
    private DifferenceResult differenceResult;
    //index 0 points to processed count, index 1 points to total
    private int scannedFileCount;
    private int totalFileCount;

    public AutoExecutorReportRecord(DifferenceResult differenceResult, int scannedFileCount, int totalFileCount) {
        this.differenceResult = differenceResult;
        this.scannedFileCount = scannedFileCount;
        this.totalFileCount = totalFileCount;
    }

    public DifferenceResult getDifferenceResult() {
        return differenceResult;
    }

    public int getScannedFileCount() {
        return scannedFileCount;
    }

    public int getTotalFileCount() { return totalFileCount; }
}
