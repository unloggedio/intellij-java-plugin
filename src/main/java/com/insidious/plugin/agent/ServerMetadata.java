package com.insidious.plugin.agent;

import java.util.Date;

public class ServerMetadata {
    String includePackageName;
    String agentVersion;
    String agentServerPort;
    private String agentServerUrl;
    String mode;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    private String hostname;
    private Date createdAt;
    private String timezone;

    public ServerMetadata() {
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getAgentServerUrl() {
        return agentServerUrl;
    }

    public void setAgentServerUrl(String agentServerUrl) {
        this.agentServerUrl = agentServerUrl;
    }

    public ServerMetadata(String includePackageName, String agentVersion) {
        this.includePackageName = includePackageName;
        this.agentVersion = agentVersion;
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
