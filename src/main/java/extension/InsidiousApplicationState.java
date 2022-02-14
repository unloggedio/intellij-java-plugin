package extension;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InsidiousApplicationState implements RunProfileState {

    public static final Key<InsidiousApplicationState> KEY = Key.create(InsidiousApplicationState.class.getName());


    @Override
    public @Nullable ExecutionResult execute(Executor executor, @NotNull ProgramRunner<?> runner) throws ExecutionException {
        @NotNull ProcessHandler processHandler = new InsidiousProcessHandler();
        DefaultExecutionResult executionResult = new DefaultExecutionResult(processHandler);
        return executionResult;
    }
}
