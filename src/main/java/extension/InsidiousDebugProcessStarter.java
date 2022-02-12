package extension;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import org.jetbrains.annotations.NotNull;

public class InsidiousDebugProcessStarter extends XDebugProcessStarter {

    private final InsidiousProgramRunner insidiousProgramRunner;
    private final RemoteConnection connection;
    private final ExecutionResult executionResult;
    private final ExecutionEnvironment environment;
    private final InsidiousRunProfileState state;
    private final long pollTimeout;

    public InsidiousDebugProcessStarter(InsidiousProgramRunner insidiousProgramRunner,
                                        RemoteConnection connection,
                                        ExecutionResult executionResult,
                                        ExecutionEnvironment environment,
                                        InsidiousRunProfileState state,
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
        InsidiousDebugProcess debugProcess = InsidiousDebugProcess.create(session);

        debugProcess.setExecutionResult(this.executionResult);


        return debugProcess;
    }
}
