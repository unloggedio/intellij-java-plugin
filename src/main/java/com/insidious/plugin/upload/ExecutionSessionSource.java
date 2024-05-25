package com.insidious.plugin.upload;

import com.insidious.plugin.constants.ExecutionSessionSourceMode;

import java.util.ArrayList;
import java.util.List;

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

    public String getServerEndpoint() {
        return this.serverEndpoint;
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
