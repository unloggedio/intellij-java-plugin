package com.insidious.plugin.factory;

import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ActiveSessionManager {


    private static final Logger logger = LoggerUtil.getInstance(ActiveSessionManager.class);
    private final Map<String, SessionInstance> sessionInstanceMap = new HashMap<>();

    public synchronized SessionInstance createSessionInstance(ExecutionSession executionSession, Project project) throws SQLException, IOException {
        if (sessionInstanceMap.containsKey(executionSession.getSessionId())) {
            return sessionInstanceMap.get(executionSession.getSessionId());
        }
        SessionInstance sessionInstance = new SessionInstance(executionSession, project);
        sessionInstanceMap.put(executionSession.getSessionId(), sessionInstance);
        return sessionInstance;
    }

    public void cleanUpSessionDirectory(ExecutionSession executionSession) {
        SessionInstance sessionInstance = sessionInstanceMap.get(executionSession.getSessionId());
        if (sessionInstance == null) {
            logger.warn("called to delete unknown session id: " + executionSession.getSessionId()
                    + " -> " + executionSession.getPath());
            return;
        }
        sessionInstance.close();
        sessionInstanceMap.remove(executionSession.getSessionId());
        deleteDirectory(FileSystems.getDefault()
                .getPath(executionSession.getPath())
                .toFile());
    }

    void deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directoryToBeDeleted.delete();
    }

}
