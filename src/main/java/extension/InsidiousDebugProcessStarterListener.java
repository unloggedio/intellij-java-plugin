package extension;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.sun.jdi.ThreadReference;
import org.jetbrains.annotations.NotNull;

public class InsidiousDebugProcessStarterListener implements InsidiousDebugProcessListener {

    private static final Logger logger = Logger.getInstance(InsidiousDebugProcessStarterListener.class);
    private final InsidiousDebugProcess insidiousDebugProcess;
    private final InsidiousApplicationState applicationState;

    public InsidiousDebugProcessStarterListener(InsidiousDebugProcess insidiousDebugProcess, InsidiousApplicationState applicationState) {
        this.insidiousDebugProcess = insidiousDebugProcess;
        this.applicationState = applicationState;
    }

    @Override
    public void connectorIsReady() {
        InsidiousDebugProcessListener.super.connectorIsReady();
    }

    @Override
    public void paused(@NotNull XSuspendContext suspendContext) {
        InsidiousDebugProcessListener.super.paused(suspendContext);
    }

    @Override
    public void resumed(XSuspendContext suspendContext) {
        InsidiousDebugProcessListener.super.resumed(suspendContext);
    }

    @Override
    public void processDetached(@NotNull XDebugProcess process, boolean closedByUser) {
        InsidiousDebugProcessListener.super.processDetached(process, closedByUser);
    }

    @Override
    public void processAttached(@NotNull XDebugProcess process) {
        InsidiousDebugProcessListener.super.processAttached(process);
    }

    @Override
    public void attachException(RunProfileState state, ExecutionException exception, RemoteConnection remoteConnection) {
        InsidiousDebugProcessListener.super.attachException(state, exception, remoteConnection);
    }

    @Override
    public void threadStarted(@NotNull XDebugProcess proc, ThreadReference thread) {
        InsidiousDebugProcessListener.super.threadStarted(proc, thread);
    }

    @Override
    public void threadStopped(@NotNull XDebugProcess proc, ThreadReference thread) {
        InsidiousDebugProcessListener.super.threadStopped(proc, thread);
    }
}
