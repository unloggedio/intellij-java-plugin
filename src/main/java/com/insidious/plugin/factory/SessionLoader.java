package com.insidious.plugin.factory;

import com.insidious.plugin.callbacks.GetProjectSessionsCallback;
import com.insidious.plugin.client.UnloggedClientInterface;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SessionLoader implements Runnable, GetProjectSessionsCallback, Disposable {


    private static final Logger logger = LoggerUtil.getInstance(SessionLoader.class);
    private final ScheduledExecutorService ourPool;
    private final List<GetProjectSessionsCallback> listeners = new ArrayList<>();
    private UnloggedClientInterface client;
    private List<ExecutionSession> lastResult = new ArrayList<>();

    public SessionLoader() {
        ourPool = Executors.newSingleThreadScheduledExecutor(new DefaultThreadFactory("UnloggedAppThreadPool"));
        ourPool.scheduleWithFixedDelay(this, 0, 3000, TimeUnit.MILLISECONDS);
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
        try {
            if (client == null) {
                return;
            }
            logger.debug("Check for new sessions");
            client.getProjectSessions(this);
        } catch (Throwable th) {
            logger.warn("Session loader error: " + th.getMessage(), th);
        }
    }

    public void addSessionCallbackListener(GetProjectSessionsCallback getProjectSessionsCallback) {
        this.listeners.add(getProjectSessionsCallback);
        if (lastResult != null) {
            getProjectSessionsCallback.success(lastResult);
        }
    }

    public void setClient(UnloggedClientInterface client) {
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
