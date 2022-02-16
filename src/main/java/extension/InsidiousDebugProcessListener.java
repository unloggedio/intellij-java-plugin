package extension;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.sun.jdi.ThreadReference;
import org.jetbrains.annotations.NotNull;

import java.util.EventListener;

public interface InsidiousDebugProcessListener extends EventListener {
    default void connectorIsReady() {
    }

    default void paused(@NotNull XSuspendContext suspendContext) {
    }

    default void resumed(XSuspendContext suspendContext) {
    }

    default void processDetached(@NotNull XDebugProcess process, boolean closedByUser) {
    }

    default void processAttached(@NotNull XDebugProcess process) {

    }

    default void attachException(RunProfileState state, ExecutionException exception, RemoteConnection remoteConnection) {
    }

    default void threadStarted(@NotNull XDebugProcess proc, ThreadReference thread) {
    }

    default void threadStopped(@NotNull XDebugProcess proc, ThreadReference thread) {
    }

}
