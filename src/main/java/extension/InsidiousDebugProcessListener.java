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
        new Exception().printStackTrace();
    }

    default void paused(@NotNull XSuspendContext suspendContext) {
        new Exception().printStackTrace();
    }

    default void resumed(XSuspendContext suspendContext) {
        new Exception().printStackTrace();
    }

    default void processDetached(@NotNull XDebugProcess process, boolean closedByUser) {
        new Exception().printStackTrace();
    }

    default void processAttached(@NotNull XDebugProcess process) {
        new Exception().printStackTrace();

    }

    default void attachException(RunProfileState state, ExecutionException exception, RemoteConnection remoteConnection) {
        new Exception().printStackTrace();
    }

    default void threadStarted(@NotNull XDebugProcess proc, ThreadReference thread) {
        new Exception().printStackTrace();
    }

    default void threadStopped(@NotNull XDebugProcess proc, ThreadReference thread) {
        new Exception().printStackTrace();
    }

}
