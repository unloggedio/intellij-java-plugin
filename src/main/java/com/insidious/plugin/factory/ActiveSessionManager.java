package com.insidious.plugin.factory;

import com.insidious.plugin.Constants;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.agent.ServerMetadata;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.client.SessionInstanceInterface;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.constants.SessionMode;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import com.insidious.plugin.client.NetworkSessionInstanceClient;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ActiveSessionManager {


    private static final Logger logger = LoggerUtil.getInstance(ActiveSessionManager.class);
    private final Map<String, SessionInstanceInterface> sessionInstanceMap = new HashMap<>();

    public ActiveSessionManager() {
    }

    public synchronized SessionInstanceInterface createSessionInstance(ExecutionSession executionSession, ServerMetadata serverMetadata, Project project) {
        if (sessionInstanceMap.containsKey(executionSession.getSessionId())) {
            return sessionInstanceMap.get(executionSession.getSessionId());
        }

        // these are set before
        SessionMode sessionMode = SessionMode.SERVER;
        String sessionNetworkUrl = "http://localhost:8123";

        // create a session instance
        SessionInstanceInterface sessionInstance;
        if (sessionMode == SessionMode.LOCAL) {
            logger.info("attempting to create a session instance from local process");
            try {
                sessionInstance = new SessionInstance(executionSession, serverMetadata, project);
             } catch (SQLException | IOException e) {
                 logger.error("Failed to initialize session instance: " + e.getMessage(), e);
                 InsidiousNotification.notifyMessage("Failed to initialize session instance: " + e.getMessage(),
                         NotificationType.ERROR);
                 throw new RuntimeException(e);
             }
        }
        else {
            logger.info("attempting to create a session instance from remote process");
            sessionInstance = new NetworkSessionInstanceClient(sessionNetworkUrl);
        }

        sessionInstanceMap.put(executionSession.getSessionId(), sessionInstance);
        return sessionInstance;
    }

    private final Map<String, Boolean> isDeletedSession = new HashMap<>();

    public synchronized void cleanUpSessionDirectory(ExecutionSession executionSession) {
        String sessionPath = executionSession.getPath();
        if (isDeletedSession.containsKey(sessionPath)) {
            return;
        }

        SessionInstanceInterface sessionInstance = sessionInstanceMap.get(executionSession.getSessionId());
        if (sessionInstance == null) {
            logger.warn("called to delete unknown session id: " + executionSession.getSessionId()
                    + " -> " + sessionPath);
        } else {
            closeSession(sessionInstance);
        }
        File directoryToBeDeleted = FileSystems.getDefault()
                .getPath(sessionPath)
                .toFile();
        if (!directoryToBeDeleted.exists()) {
            return;
        }
        logger.warn("Deleting directory: " + directoryToBeDeleted);
        isDeletedSession.put(sessionPath, true);
        deleteDirectory(directoryToBeDeleted);
    }

    void deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
//        logger.warn("[1] Delete file: " + directoryToBeDeleted.getAbsolutePath());
        boolean wasDeleted = directoryToBeDeleted.delete();
        if (!wasDeleted) {
            logger.warn("Failed to deleted [" + directoryToBeDeleted.getAbsolutePath() + "]");
        }
    }

    public void closeSession(SessionInstanceInterface sessionInstance) {
        sessionInstanceMap.remove(sessionInstance.getExecutionSession().getSessionId());
        sessionInstance.close();
    }
}
