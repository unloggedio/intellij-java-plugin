package com.insidious.plugin.factory;

import com.insidious.plugin.callbacks.GetProjectSessionsCallback;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SessionLoader implements Runnable, GetProjectSessionsCallback, Disposable {


    private static final Logger logger = LoggerUtil.getInstance(SessionLoader.class);
    private final ExecutorService ourPool;
    private final List<GetProjectSessionsCallback> listeners = new ArrayList<>();
    private VideobugClientInterface client;
    private List<ExecutionSession> lastResult;

    public SessionLoader() {
        ourPool = Executors.newFixedThreadPool(1, new DefaultThreadFactory("UnloggedAppThreadPool"));
        ourPool.submit(this);
    }

    @Override
    public void error(String message) {
        logger.warn("Failed to get sessions: " + message);
    }

    @Override
    public void success(List<ExecutionSession> executionSessionList) {
        lastResult = executionSessionList;
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
        if (lastResult != null) {
            getProjectSessionsCallback.success(lastResult);
        }
    }

    public void setClient(VideobugClientInterface client) {
        this.client = client;
    }

    public void removeListener(GetProjectSessionsCallback getProjectSessionsCallback) {
        this.listeners.remove(getProjectSessionsCallback);
    }

    @Override
    public void dispose() {
        ourPool.shutdown();
    }
}
