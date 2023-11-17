package com.insidious.plugin.factory;

import com.insidious.plugin.callbacks.GetProjectSessionsCallback;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SessionLoader implements Runnable, GetProjectSessionsCallback {


    private static final Logger logger = LoggerUtil.getInstance(SessionLoader.class);
    private final ExecutorService ourPool;
    private List<GetProjectSessionsCallback> listeners = new ArrayList<>();
    private VideobugClientInterface client;

    public SessionLoader() {
        ourPool = Executors.newFixedThreadPool(1);
        ourPool.submit(this);
    }

    @Override
    public void error(String message) {
        logger.warn("Failed to get sessions: " + message);
    }

    @Override
    public void success(List<ExecutionSession> executionSessionList) {

        for (GetProjectSessionsCallback listener : listeners) {
            listener.success(executionSessionList);
        }

    }


    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(3000);
                if (client == null) {
                    continue;
                }
                logger.debug("Check for new sessions");
                client.getProjectSessions(this);
            } catch (InterruptedException ie) {
                logger.warn("Session loader interrupted: " + ie.getMessage());
                break;
            }
        }
    }

    public void addSessionCallbackListener(GetProjectSessionsCallback getProjectSessionsCallback) {
        this.listeners.add(getProjectSessionsCallback);
    }

    public void setClient(VideobugClientInterface client) {
        this.client = client;
    }

    public void removeListener(GetProjectSessionsCallback getProjectSessionsCallback) {
        this.listeners.remove(getProjectSessionsCallback);
    }
}
