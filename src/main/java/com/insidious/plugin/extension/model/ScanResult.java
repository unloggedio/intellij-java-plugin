package com.insidious.plugin.extension.model;

public class ScanResult {
    Integer index;
    Integer callStack;
    private boolean matched;

    public ScanResult(Integer index, int callStack) {
        this.index = index;
        this.callStack = callStack;
        this.matched= false;
    }

    public Integer getIndex() {
        return index;
    }

    public Integer getCallStack() {
        return callStack;
    }

    public ScanResult(Integer index, Integer callStack, boolean matched) {
        this.index = index;
        this.callStack = callStack;
        this.matched = matched;
    }

    public boolean matched() {
        return matched;
    }
}
