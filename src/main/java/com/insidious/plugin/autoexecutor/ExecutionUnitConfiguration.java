package com.insidious.plugin.autoexecutor;

import com.intellij.openapi.vfs.VirtualFile;

import java.util.List;

public class ExecutionUnitConfiguration {
    private String executorId;
    private List<VirtualFile> payload;
    private long queueCapacity;
    private boolean useMocks;

    public String getExecutorId() {
        return executorId;
    }

    public List<VirtualFile> getPayload() {
        return payload;
    }

    public long getQueueCapacity() {
        return queueCapacity;
    }

    public boolean isUseMocks() {
        return useMocks;
    }

    public ExecutionUnitConfiguration(String executorId, List<VirtualFile> payload,
                                      long queueCapacity, boolean useMocks) {
        this.executorId = executorId;
        this.payload = payload;
        this.queueCapacity = queueCapacity;
        this.useMocks = useMocks;
    }
}
