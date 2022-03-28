package com.insidious.plugin.extension;

import com.insidious.plugin.network.pojo.exceptions.APICallException;
import com.insidious.plugin.network.pojo.exceptions.UnauthorizedException;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.ui.Messages;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;

public class InsidiousDebugProcessStarter extends XDebugProcessStarter {

    private static final Logger logger = LoggerUtil.getInstance(InsidiousDebugProcessStarter.class);
    private final InsidiousProgramRunner insidiousProgramRunner;
    private final RemoteConnection connection;
    private final ExecutionResult executionResult;
    private final ExecutionEnvironment environment;
    private final InsidiousApplicationState state;
    private final long pollTimeout;

    public InsidiousDebugProcessStarter(InsidiousProgramRunner insidiousProgramRunner,
                                        RemoteConnection connection,
                                        ExecutionResult executionResult,
                                        ExecutionEnvironment environment,
                                        InsidiousApplicationState state,
                                        long pollTimeout) {
        this.insidiousProgramRunner = insidiousProgramRunner;
        this.connection = connection;
        this.executionResult = executionResult;
        this.environment = environment;
        this.state = state;
        this.pollTimeout = pollTimeout;
    }

    @Override
    public @NotNull XDebugProcess start(@NotNull XDebugSession session) throws ExecutionException {
        InsidiousJavaDebugProcess debugProcess = null;
        try {
            debugProcess = InsidiousJavaDebugProcess.create(session, connection);
        } catch (UnauthorizedException e) {
            logger.error("failed to fetch project sessions", e);
            e.printStackTrace();
            throw new ExecutionException(e);
        } catch (APICallException e) {
            logger.error("failed to fetch project sessions", e);
            e.printStackTrace();
            Messages.showErrorDialog(session.getProject(), e.getMessage(), "Failed to start VideoBug Session");
            throw new ExecutionException(e);
        } catch (IOException e) {
            logger.error("failed to fetch project sessions", e);
            e.printStackTrace();
            Messages.showErrorDialog(session.getProject(), "Failed to connect with server - " + e.getMessage(), "Failed");
            throw new ExecutionException(e);
        } catch (Exception e) {
            logger.error("failed to fetch project sessions", e);
            e.printStackTrace();
            Messages.showErrorDialog(session.getProject(), "Failed to connect with server - " + e.getMessage(), "Failed");
            throw new ExecutionException(e);
        }

        debugProcess.setExecutionResult(this.executionResult);
        try {
            debugProcess.attachVM("100");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return debugProcess;
    }
}
