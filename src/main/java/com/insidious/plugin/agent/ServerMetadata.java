package com.insidious.plugin.agent;

public class ServerMetadata {
    String includePackageName;
    String agentVersion;
    String agentServerPort;

    public String getAgentServerUrl() {
        return agentServerUrl;
    }

    public void setAgentServerUrl(String agentServerUrl) {
        this.agentServerUrl = agentServerUrl;
    }

    String agentServerUrl;

    public ServerMetadata(String includePackageName, String agentVersion) {
        this.includePackageName = includePackageName;
        this.agentVersion = agentVersion;
    }

    public ServerMetadata() {
    }

    public String getAgentServerPort() {
        return agentServerPort;
    }

    public void setAgentServerPort(String agentServerPort) {
        this.agentServerPort = agentServerPort;
    }

    public String getIncludePackageName() {
        return includePackageName;
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

    @Override
    public String toString() {
        return "ServerMetadata{" +
                "includePackageName='" + includePackageName + '\'' +
                ", agentVersion='" + agentVersion + '\'' +
                '}';
    }
}
