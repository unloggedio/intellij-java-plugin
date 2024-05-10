package com.insidious.plugin.client.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExecutionSession {


    public static final String EXECUTION_DB_NAME = "execution.db";
    public static final String LOG_FILE_NAME = "log.txt";
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

    public String getDatabaseConnectionString() {
        return "jdbc:sqlite:" + Path.of(path, EXECUTION_DB_NAME);
    }

    public String getDatabasePath() {
        return FileSystems.getDefault().getPath(path, EXECUTION_DB_NAME).toAbsolutePath().toString();
    }

    public String getLogFilePath() {
        return FileSystems.getDefault().getPath(path, LOG_FILE_NAME).toAbsolutePath().toString();
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
