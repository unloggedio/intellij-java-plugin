package com.insidious.plugin.extension;

import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.client.pojo.exceptions.UnauthorizedException;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import org.jetbrains.annotations.NotNull;

public class InsidiousDebugProcessStarter extends XDebugProcessStarter {

    private static final Logger logger = LoggerUtil.getInstance(InsidiousDebugProcessStarter.class);
    private final RemoteConnection connection;
    private final ExecutionResult executionResult;
    private final InsidiousApplicationState state;

    public InsidiousDebugProcessStarter(
            RemoteConnection connection,
            ExecutionResult executionResult,
            InsidiousApplicationState state) {
        this.connection = connection;
        this.executionResult = executionResult;
        this.state = state;
    }

    @Override
    public @NotNull XDebugProcess start(@NotNull XDebugSession session) throws ExecutionException {
        InsidiousJavaDebugProcess debugProcess = null;
        try {
            debugProcess = InsidiousJavaDebugProcess.create(session, connection);

            InsidiousDebugProcessStarterListener listener =
                    new InsidiousDebugProcessStarterListener(debugProcess, this.state, this.connection);
            debugProcess.addDebugProcessListener(listener);
            debugProcess.setExecutionResult(executionResult);

        } catch (UnauthorizedException e) {
            logger.error("failed to fetch project sessions", e);
            e.printStackTrace();
            throw new ExecutionException(e);
        } catch (APICallException e) {
            logger.error("failed to fetch project sessions", e);
            e.printStackTrace();
            Messages.showErrorDialog(session.getProject(), e.getMessage(), "Failed to start VideoBug Session");
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
        } catch (Exception e) {
            logger.error("failed to attach to vm", e);
        }

        return debugProcess;
    }
}
