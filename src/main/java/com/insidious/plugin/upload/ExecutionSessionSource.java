package com.insidious.plugin.upload;

import com.insidious.plugin.constants.ExecutionSessionSourceMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExecutionSessionSource {
    private ExecutionSessionSourceMode sessionSourceMode = ExecutionSessionSourceMode.LOCAL;
    private String serverEndpoint = "http://unlogged.local:8123";
    private SourceFilter sourceFilter = SourceFilter.SELECTED_ONLY;
    private List<String> sessionId = new ArrayList<>();

    public ExecutionSessionSource(ExecutionSessionSourceMode sessionSourceMode) {
        this.sessionSourceMode = sessionSourceMode;
    }

    public ExecutionSessionSource() {
    }

    public ExecutionSessionSource(ExecutionSessionSource copyFrom) {
        this.sessionSourceMode = copyFrom.sessionSourceMode;
        this.serverEndpoint = copyFrom.serverEndpoint;
        this.sourceFilter = copyFrom.sourceFilter;
        this.sessionId = new ArrayList<>(copyFrom.sessionId);
    }

    public String getServerEndpoint() {
        return this.serverEndpoint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExecutionSessionSource)) return false;
        ExecutionSessionSource that = (ExecutionSessionSource) o;
        return sessionSourceMode == that.sessionSourceMode && Objects.equals(serverEndpoint,
                that.serverEndpoint) && sourceFilter == that.sourceFilter && Objects.equals(sessionId,
                that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionSourceMode, serverEndpoint, sourceFilter, sessionId);
    }

    public void setServerEndpoint(String serverEndpoint) {
        this.serverEndpoint = serverEndpoint;
    }

    public SourceFilter getSourceFilter() {
        return this.sourceFilter;
    }

    public void setSourceFilter(SourceFilter sourceFilter) {
        this.sourceFilter = sourceFilter;
    }

    public List<String> getSessionId() {
        return this.sessionId;
    }

    public void setSessionId(List<String> sessionId) {
        this.sessionId = sessionId;
    }

    public ExecutionSessionSourceMode getSessionMode() {
        return this.sessionSourceMode;
    }

    public void setSessionMode(ExecutionSessionSourceMode executionSessionSourceMode) {
        this.sessionSourceMode = executionSessionSourceMode;
    }
}
