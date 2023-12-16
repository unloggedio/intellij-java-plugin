package com.insidious.plugin.pojo;

public class ReplayAllExecutionContext {
    private String source;
    private boolean savedCandidateCentricFlow;

    public ReplayAllExecutionContext(String source, boolean savedCandidateCentricFlow) {
        this.source = source;
        this.savedCandidateCentricFlow = savedCandidateCentricFlow;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isSavedCandidateCentricFlow() {
        return savedCandidateCentricFlow;
    }

    public void setSavedCandidateCentricFlow(boolean savedCandidateCentricFlow) {
        this.savedCandidateCentricFlow = savedCandidateCentricFlow;
    }
}
