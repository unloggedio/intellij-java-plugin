package com.insidious.plugin.client.pojo;


import java.nio.file.Path;
import java.util.Date;

public class ExecutionSession {


    public static final String EXECUTION_DB_NAME = "execution.db";
    private long lastUpdateAt;
    private String projectId;
    private Date createdAt;
    private String sessionId;
    private String hostname;
    private String path;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public long getLastUpdateAt() {
        return lastUpdateAt;
    }

    public void setLastUpdateAt(long lastUpdateAt) {
        this.lastUpdateAt = lastUpdateAt;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "[" + hostname + "] " +
                sessionId + " - " + createdAt;
    }

    public String getDatabasePath() {
        return "jdbc:sqlite:" + Path.of(path, EXECUTION_DB_NAME);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
