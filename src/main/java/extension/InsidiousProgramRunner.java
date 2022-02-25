package extension;

import com.intellij.debugger.impl.GenericDebuggerRunner;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.target.TargetEnvironmentAwareRunProfileState;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.xdebugger.XDebuggerManager;
import factory.ProjectService;
import network.pojo.exceptions.UnauthorizedException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class InsidiousProgramRunner extends GenericDebuggerRunner {

    @Override
    public @NotNull @NonNls String getRunnerId() {
        return "insidious-runner";
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

        @NotNull String moduleName = ModuleManager.getInstance(env.getProject()).getModules()[0].getName();

        RemoteConnection connection;

        try {
            connection = new RemoteConnection("http://localhost:8080", env.getProject().getService(ProjectService.class).getClient());
            ((InsidiousApplicationState) state).setCommandSender(new CommandSender(connection));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (UnauthorizedException e) {
            env.getProject().getService(ProjectService.class).showCredentialsWindow();
            return null;
        }
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


    @Override
    protected @NotNull Promise<@Nullable RunContentDescriptor> doExecuteAsync(@NotNull TargetEnvironmentAwareRunProfileState state, @NotNull ExecutionEnvironment env) throws ExecutionException {
        return super.doExecuteAsync(state, env);
    }

    @Override
    public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
        super.execute(environment);
    }
}
