package extension;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class InsidiousDebugProcessStarter extends XDebugProcessStarter {

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
        InsidiousJavaDebugProcess debugProcess = InsidiousJavaDebugProcess.create(session, connection);

        debugProcess.setExecutionResult(this.executionResult);
        try {
            debugProcess.attachVM("100");
        } catch (IOException e) {
            e.printStackTrace();
        }
        debugProcess.startPausing();
        return debugProcess;
    }
}
