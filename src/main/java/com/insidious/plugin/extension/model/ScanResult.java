package com.insidious.plugin.extension.model;

public class ScanResult {
    Integer index;
    Integer callStack;

    public Integer getIndex() {
        return index;
    }

    public Integer getCallStack() {
        return callStack;
    }

    public ScanResult(Integer index, Integer callStack) {
        this.index = index;
        this.callStack = callStack;
    }
}
