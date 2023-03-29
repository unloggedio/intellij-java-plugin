package com.insidious.plugin.factory;

import com.insidious.plugin.callbacks.GetProjectSessionsCallback;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class SessionLoader implements Runnable, GetProjectSessionsCallback {


    private static final Logger logger = LoggerUtil.getInstance(SessionLoader.class);
    final InsidiousService insidiousService;
    private final VideobugClientInterface client;
    private ExecutionSession currentSession = null;

    public SessionLoader(VideobugClientInterface videobugClientInterface, InsidiousService insidiousService) {
        this.client = videobugClientInterface;
        this.insidiousService = insidiousService;
        client.getProjectSessions(this);
    }

    @Override
    public void error(String message) {
        logger.warn("Failed to get sessions: " + message);
    }

    @Override
    public void success(List<ExecutionSession> executionSessionList) {
        try {
            if (executionSessionList.size() == 0) {
                // the currently loaded session has been deleted
                if (currentSession != null && currentSession.getSessionId().equals("na")) {
                    // already na is set
                    return;
                }

                insidiousService.loadDefaultSession();
                currentSession = insidiousService.getCurrentExecutionSession();
                return;

            }
            ExecutionSession mostRecentSession = executionSessionList.get(executionSessionList.size() - 1);
            if (currentSession == null) {
                // no session currently loaded and we can load a new sessions
                currentSession = mostRecentSession;
                insidiousService.setSession(mostRecentSession);

            } else if (!currentSession.getSessionId().equals(mostRecentSession.getSessionId())) {
                logger.warn("Current loaded session [" + currentSession.getSessionId() + "] is different from most " +
                        "recent session found [" + mostRecentSession.getSessionId() + "]");
                currentSession = mostRecentSession;
                insidiousService.setSession(mostRecentSession);
            }
        } catch (SQLException | IOException e) {
            logger.error("Failed to set new session: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(1000);
                client.getProjectSessions(this);
            } catch (InterruptedException ie) {
                break;
            }
        }
    }

}
