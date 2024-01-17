package com.insidious.plugin.autoexecutor;

import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.ui.methodscope.DifferenceResult;

import java.util.List;

public class AutoExecutorReportRecord {
    private DifferenceResult differenceResult;
    private int scannedFileCount;
    private int totalFileCount;
    private List<DeclaredMock> declaredMockList;
    private String source;
    private String methodReturnTypeCannonicalText;
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

    public String getMethodReturnTypeCannonicalText() {
        return methodReturnTypeCannonicalText;
    }

    public void setMethodReturnTypeCannonicalText(String methodReturnTypeCannonicalText) {
        this.methodReturnTypeCannonicalText = methodReturnTypeCannonicalText;
    }
}
