package com.insidious.plugin.agent;

public class ServerMetadata {
    String includePackageName;
    String agentVersion;

    public ServerMetadata(String includePackageName, String agentVersion) {
        this.includePackageName = includePackageName;
        this.agentVersion = agentVersion;
    }

    public String getIncludePackageName() {
        return includePackageName;
    }

    public ServerMetadata() {
    }

    public void setIncludePackageName(String includePackageName) {
        this.includePackageName = includePackageName;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
    }
}
