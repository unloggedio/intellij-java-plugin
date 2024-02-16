package com.insidious.plugin.pojo;

import com.insidious.plugin.callbacks.ExecutionRequestSourceType;

public class ReplayAllExecutionContext {
    private ExecutionRequestSourceType source;
    private boolean savedCandidateCentricFlow;

    public ReplayAllExecutionContext(ExecutionRequestSourceType source, boolean savedCandidateCentricFlow) {
        this.source = source;
        this.savedCandidateCentricFlow = savedCandidateCentricFlow;
    }

    public ExecutionRequestSourceType getSource() {
        return source;
    }

    public void setSource(ExecutionRequestSourceType source) {
        this.source = source;
    }

    public boolean isSavedCandidateCentricFlow() {
        return savedCandidateCentricFlow;
    }

    public void setSavedCandidateCentricFlow(boolean savedCandidateCentricFlow) {
        this.savedCandidateCentricFlow = savedCandidateCentricFlow;
    }
}
