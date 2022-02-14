package extension;

import com.intellij.debugger.impl.GenericDebuggerRunner;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.xdebugger.XDebuggerManager;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

public class InsidiousProgramRunner extends GenericDebuggerRunner {

    @Override
    public @NotNull @NonNls String getRunnerId() {
        return "runner-id";
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        // only run our quarkus config type
        if (profile instanceof RunConfiguration && executorId.equals("InsidiousTimeTravel"))
            return ((RunConfiguration) profile).getType() instanceof InsidiousRunConfigTypeInterface;
        return false;
    }

    @Nullable
    @Override
    protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment env) throws ExecutionException {
        RemoteConnection connection = new RemoteConnection(false, "InsidiousTimeTravelServer", "InsidiousTimeTravelServer", true);
        boolean pollTimeout = false;

        AtomicReference<ExecutionException> ex = new AtomicReference<>();
        AtomicReference<RunContentDescriptor> result = new AtomicReference<>();
        ApplicationManager.getApplication().invokeAndWait(() -> {
            try {
                @Nullable ExecutionResult executionResult = state.execute(env.getExecutor(), env.getRunner());

                InsidiousDebugProcessStarter processStarter = new InsidiousDebugProcessStarter(
                        this, connection, executionResult, env, (InsidiousApplicationState) state, 1000);
                result.set(XDebuggerManager.getInstance(env.getProject()).startSession(env, processStarter).getRunContentDescriptor());
            } catch (ExecutionException e) {
                ex.set(e);
            }
        });
        if (ex.get() != null) throw ex.get();
        return result.get();
    }
}
