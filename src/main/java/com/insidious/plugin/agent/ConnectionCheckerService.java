package com.insidious.plugin.agent;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;

public class ConnectionCheckerService implements Runnable {

    private final static Logger logger = LoggerUtil.getInstance(ConnectionCheckerService.class);
    private final UnloggedSdkApiAgent unloggedSdkApiAgent;
    private boolean currentState = false;

    public ConnectionCheckerService(UnloggedSdkApiAgent unloggedSdkApiAgent) {
        this.unloggedSdkApiAgent = unloggedSdkApiAgent;
    }

    @Override
    public void run() {

        AgentConnectionStateNotifier applicationLevelPublisher = ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(AgentConnectionStateNotifier.TOPIC_AGENT_CONNECTION_STATE);
        while (true) {
            AgentCommandResponse<ServerMetadata> response = unloggedSdkApiAgent.ping();
            logger.debug("Agent ping response: " + response.getResponseType());
            boolean newState = response.getResponseType().equals(ResponseType.NORMAL);
            if (newState && !currentState) {
                currentState = true;
                ServerMetadata serverMetadata = response.getMethodReturnValue();
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    applicationLevelPublisher.onConnectedToAgentServer(serverMetadata);
                });
            } else if (!newState && currentState) {
                currentState = false;
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    applicationLevelPublisher.onDisconnectedFromAgentServer();
                });
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}