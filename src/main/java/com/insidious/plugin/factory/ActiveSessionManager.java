package com.insidious.plugin.factory;

import com.insidious.plugin.Constants;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ActiveSessionManager {


    private static final Logger logger = LoggerUtil.getInstance(ActiveSessionManager.class);
    private final Map<String, SessionInstance> sessionInstanceMap = new HashMap<>();
    private final ExecutionSession defaultSessionInstance;

    public ActiveSessionManager() {
        String pathToSessions = Constants.HOME_PATH + "/sessions/na";

        defaultSessionInstance = new ExecutionSession();
        defaultSessionInstance.setPath(pathToSessions);
        defaultSessionInstance.setSessionId("na");
        defaultSessionInstance.setCreatedAt(new Date());
        defaultSessionInstance.setLastUpdateAt(new Date().getTime());

    }

    public synchronized SessionInstance createSessionInstance(ExecutionSession executionSession, Project project) {
        if (sessionInstanceMap.containsKey(executionSession.getSessionId())) {
            return sessionInstanceMap.get(executionSession.getSessionId());
        }
        SessionInstance sessionInstance = null;
        try {
            sessionInstance = new SessionInstance(executionSession, project);
        } catch (SQLException | IOException e) {
            logger.error("Failed to initialize session instance: " + e.getMessage(), e);
            InsidiousNotification.notifyMessage("Failed to initialize session instance: " + e.getMessage(),
                    NotificationType.ERROR);
            throw new RuntimeException(e);
        }
        sessionInstanceMap.put(executionSession.getSessionId(), sessionInstance);
        return sessionInstance;
    }

    public ExecutionSession loadDefaultSession() {
        return defaultSessionInstance;
    }


    public void cleanUpSessionDirectory(ExecutionSession executionSession) {
        SessionInstance sessionInstance = sessionInstanceMap.get(executionSession.getSessionId());
        if (sessionInstance == null) {
            logger.warn("called to delete unknown session id: " + executionSession.getSessionId()
                    + " -> " + executionSession.getPath());
        } else {
            sessionInstance.close();
            sessionInstanceMap.remove(executionSession.getSessionId());
        }
        File directoryToBeDeleted = FileSystems.getDefault()
                .getPath(executionSession.getPath())
                .toFile();
        if (!directoryToBeDeleted.exists()) {
            return;
        }
        logger.warn("Deleting directory: " + directoryToBeDeleted);
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
        directoryToBeDeleted.delete();
    }

}
