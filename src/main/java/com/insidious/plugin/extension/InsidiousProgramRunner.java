package com.insidious.plugin.extension;

import com.intellij.debugger.impl.GenericDebuggerRunner;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.module.ModuleManager;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        return true;
    }

    @Nullable
    @Override
    protected RunContentDescriptor doExecute(@NotNull RunProfileState state,
                                             @NotNull ExecutionEnvironment env) throws ExecutionException {

        @NotNull String moduleName = ModuleManager.getInstance(env.getProject())
                .getModules()[0].getName();

        RemoteConnection connection = null;
//        try {
//            VideobugClientInterface client = ApplicationManager.getApplication().getService(InsidiousService.class).getClient();
        connection = new RemoteConnection(null);
//            ((InsidiousApplicationState) state).setCommandSender(new CommandSender(connection));
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null;
//        } catch (UnauthorizedException e) {
//            ApplicationManager.getApplication().getService(InsidiousService.class).showCredentialsWindow();
//            return null;
//        }
        boolean pollTimeout = false;

        AtomicReference<ExecutionException> ex = new AtomicReference<>();
        AtomicReference<RunContentDescriptor> result = new AtomicReference<>();
//        ApplicationManager.getApplication().invokeAndWait(() -> {
//            try {
//                @Nullable ExecutionResult executionResult = state.execute(env.getExecutor(), env.getRunner());
//
//                InsidiousDebugProcessStarter processStarter =
//                        new InsidiousDebugProcessStarter(null, executionResult, (InsidiousApplicationState) state);
//                XDebugSession xDebugSession = XDebuggerManager.getInstance(env.getProject()).startSession(env, processStarter);
//                result.set(xDebugSession.getRunContentDescriptor());
//            } catch (Throwable e) {
//                ex.set(new ExecutionException(e));
//            }
//        });
        if (ex.get() != null) throw ex.get();
        return result.get();
    }

    @Override
    public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
//        super.execute(environment);
    }
}
