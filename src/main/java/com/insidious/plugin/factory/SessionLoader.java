package com.insidious.plugin.factory;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.callbacks.GetProjectSessionsCallback;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiPackage;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SessionLoader implements Runnable, GetProjectSessionsCallback {


    private static final Logger logger = LoggerUtil.getInstance(SessionLoader.class);
    final InsidiousService insidiousService;
    private final VideobugClientInterface client;
    private final Map<String, Boolean> checkCache = new HashMap<>();
    private ExecutionSession currentSession = null;

    public SessionLoader(VideobugClientInterface videobugClientInterface, InsidiousService insidiousService) {
        this.client = videobugClientInterface;
        this.insidiousService = insidiousService;
        this.insidiousService.getProject().getService(DumbService.class)
                .runWhenSmart(() -> client.getProjectSessions(SessionLoader.this));
    }

    @Override
    public void error(String message) {
        logger.warn("Failed to get sessions: " + message);
    }

    @Override
    public void success(List<ExecutionSession> executionSessionList) {
        try {
            if (executionSessionList.size() == 0) {
                logger.debug("no sessions found");
                // the currently loaded session has been deleted
                if (currentSession != null && currentSession.getSessionId().equals("na")) {
                    // already na is set
                    return;
                }

                insidiousService.loadDefaultSession();
                currentSession = insidiousService.getCurrentExecutionSession();
                return;

            }
            ExecutionSession mostRecentSession = executionSessionList.get(0);
            logger.debug(
                    "New session: [" + mostRecentSession.getSessionId() + "] vs existing session: " + currentSession);

            if (currentSession == null) {
                // no session currently loaded and we can load a new sessions
                if (!checkSessionBelongsToProject(mostRecentSession, insidiousService.getProject())) {
                    return;
                }
                currentSession = mostRecentSession;
                insidiousService.setSession(mostRecentSession);

            } else if (!currentSession.getSessionId().equals(mostRecentSession.getSessionId())) {
                if (!checkSessionBelongsToProject(mostRecentSession, insidiousService.getProject())) {
                    return;
                }
                logger.warn("Current loaded session [" + currentSession.getSessionId() + "] is different from most " +
                        "recent session found [" + mostRecentSession.getSessionId() + "]");
                currentSession = mostRecentSession;
                insidiousService.setSession(mostRecentSession);
            }
        } catch (SQLException | IOException e) {
            logger.error("Failed to set new session: " + e.getMessage(), e);
            InsidiousNotification.notifyMessage("Failed to process new session: " + e.getMessage(),
                    NotificationType.ERROR);
//            throw new RuntimeException(e);
        }
    }

    private boolean checkSessionBelongsToProject(ExecutionSession mostRecentSession, Project project) {
        if (checkCache.containsKey(mostRecentSession.getSessionId())) {
            return checkCache.get(mostRecentSession.getSessionId());
        }
        try {
            String executionLogFile = mostRecentSession.getLogFilePath();
            File logFile = new File(executionLogFile);
            BufferedReader logFileInputStream = new BufferedReader(
                    new InputStreamReader(Files.newInputStream(logFile.toPath())));
            // do not remove
            String javaVersionLine = logFileInputStream.readLine();
            String agentVersionLine = logFileInputStream.readLine();
            String agentParamsLine = logFileInputStream.readLine();
            if (!agentParamsLine.startsWith("Params: ")) {
                logger.warn(
                        "The third line is not Params line, marked as session not matching: " + mostRecentSession.getLogFilePath());
                checkCache.put(mostRecentSession.getSessionId(), false);
                return false;
            }
            String[] paramParts = agentParamsLine.substring("Params: ".length()).split(",");
            boolean foundIncludePackage = false;
            String includedPackagedName = null;
            for (String paramPart : paramParts) {
                if (paramPart.startsWith("i=")) {
                    foundIncludePackage = true;
                    includedPackagedName = paramPart.substring("i=".length());
                    break;
                }
            }
            if (!foundIncludePackage) {
                checkCache.put(mostRecentSession.getSessionId(), false);
                logger.warn(
                        "Package not found in the params, marked as session not matching" + mostRecentSession.getLogFilePath());
                return false;
            }

            String finalIncludedPackagedName = includedPackagedName.replace('/', '.');
             PsiPackage locatedPackage = ApplicationManager.getApplication().runReadAction(
                    (Computable<PsiPackage>) () -> JavaPsiFacade.getInstance(project)
                            .findPackage(finalIncludedPackagedName));
            if (locatedPackage == null) {
                logger.warn("Package for agent [" + finalIncludedPackagedName + "] NOTFOUND in current " +
                        "project [" + project.getName() + "]" +
                        " -> " + mostRecentSession.getLogFilePath());
                checkCache.put(mostRecentSession.getSessionId(), false);
                return false;
            }
            logger.warn("Package for agent [" + finalIncludedPackagedName + "] FOUND in current " +
                    "project [" + project.getName() + "]" +
                    " -> " + mostRecentSession.getLogFilePath());


            checkCache.put(mostRecentSession.getSessionId(), true);
            return true;


        } catch (Exception e) {
            return false;
        }

    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(3000);
                logger.debug("Check for new sessions");
                client.getProjectSessions(this);
            } catch (InterruptedException ie) {
                logger.warn("Session loader interrupted: " + ie.getMessage());
                break;
            }
        }
    }

}
