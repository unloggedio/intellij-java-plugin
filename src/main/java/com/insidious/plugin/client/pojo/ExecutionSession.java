package com.insidious.plugin.client.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.agent.ServerMetadata;
import com.insidious.plugin.constants.ExecutionSessionSourceMode;
import com.insidious.plugin.util.ObjectMapperInstance;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.time.ZoneId;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExecutionSession {

    private static ObjectMapper objectMapper = ObjectMapperInstance.getInstance();
    public static final String EXECUTION_DB_NAME = "execution.db";
    public static final String LOG_FILE_NAME = "log.txt";
    private long lastUpdateAt;
    private String projectId;
    private Date createdAt;
    private String sessionId;
    private String hostname;
    private String path;
    private ExecutionSessionSourceMode executionSessionSourceMode;

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

    public ExecutionSessionSourceMode getSessionMode() {
        if (this.executionSessionSourceMode == null) {
            this.executionSessionSourceMode = ExecutionSessionSourceMode.LOCAL;
        }
        return this.executionSessionSourceMode;
    }

    public void setSessionMode(ExecutionSessionSourceMode executionSessionSourceMode) {
        this.executionSessionSourceMode = executionSessionSourceMode;
    }

    @Override
    public String toString() {
        return "[" + executionSessionSourceMode + "][" + hostname + "]" +
                sessionId + "@" + createdAt;
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

    public Date getTimestampInUTC() {

        File logFile = new File(this.getLogFilePath());
        String serverMetadataJson = null;
        try {
            BufferedReader logFileInputStream = new BufferedReader(
                    new InputStreamReader(Files.newInputStream(logFile.toPath())));
            // do not remove
            String javaVersionLine = logFileInputStream.readLine();
            String agentVersionLine = logFileInputStream.readLine();
            String agentParamsLine = logFileInputStream.readLine();
            serverMetadataJson = logFileInputStream.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ServerMetadata serverMetadata = null;
        try {
            serverMetadata = objectMapper.readValue(serverMetadataJson,
                        ServerMetadata.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // calculate the timestamp in UTC timezone
        Date timestampLocalTZ = serverMetadata.getCreatedAt();
        String timezone = serverMetadata.getTimezone();

        Date createTimestampUTC = Date.from(timestampLocalTZ
                .toInstant()
                .atZone(ZoneId.of(timezone))
                .withZoneSameInstant(ZoneId.of("UTC"))
                .toInstant()
        );

        return createTimestampUTC;
    }
}
