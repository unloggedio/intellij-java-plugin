package com.insidious.plugin.autoexecutor;

public class AutoExecutorRunOptions {
    private boolean useMocks;

    public AutoExecutorRunOptions(boolean useMocks) {
        this.useMocks = useMocks;
    }

    public boolean isUseMocks() {
        return useMocks;
    }
}
