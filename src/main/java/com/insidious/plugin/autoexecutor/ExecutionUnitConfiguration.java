package com.insidious.plugin.autoexecutor;

import com.intellij.openapi.vfs.VirtualFile;

import java.util.List;

public class ExecutionUnitConfiguration {
    private String executorId;
    private List<VirtualFile> payload;
    private long queueCapacity;
    private boolean useMocks;
    private String includePackage;

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

    public String getIncludePackage() {
        return includePackage;
    }

    public ExecutionUnitConfiguration(String executorId, List<VirtualFile> payload, String includePackage,
                                      long queueCapacity, boolean useMocks) {
        this.executorId = executorId;
        this.includePackage = includePackage;
        this.payload = payload;
        this.queueCapacity = queueCapacity;
        this.useMocks = useMocks;
    }
}
