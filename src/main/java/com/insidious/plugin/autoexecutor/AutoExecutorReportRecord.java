package com.insidious.plugin.autoexecutor;

import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.ui.methodscope.DifferenceResult;

import java.util.List;

public class AutoExecutorReportRecord {
    private DifferenceResult differenceResult;
    //index 0 points to processed count, index 1 points to total
    private int scannedFileCount;
    private int totalFileCount;
    private List<DeclaredMock> declaredMockList;
    private String source;

    public AutoExecutorReportRecord(DifferenceResult differenceResult,
                                    int scannedFileCount, int totalFileCount,
                                    List<DeclaredMock> declaredMocks) {
        this.differenceResult = differenceResult;
        this.scannedFileCount = scannedFileCount;
        this.totalFileCount = totalFileCount;
        this.declaredMockList = declaredMocks;
    }

    public DifferenceResult getDifferenceResult() {
        return differenceResult;
    }

    public int getScannedFileCount() {
        return scannedFileCount;
    }

    public int getTotalFileCount() {
        return totalFileCount;
    }

    public List<DeclaredMock> getDeclaredMockList() {
        return declaredMockList;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
