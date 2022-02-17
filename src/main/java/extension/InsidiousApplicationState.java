package extension;

import com.intellij.execution.*;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.util.Key;
import com.sun.jdi.ThreadReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InsidiousApplicationState implements RunProfileState {

    public static final Key<InsidiousApplicationState> KEY = Key.create(InsidiousApplicationState.class.getName());
    private final InsidiousRunConfiguration configuration;
    private final ExecutionEnvironment environment;
    private ConsoleView consoleView;
    private ThreadReference initialThread;
    private TextConsoleBuilder consoleBuilder;


    public InsidiousApplicationState(@NotNull InsidiousRunConfiguration configuration, ExecutionEnvironment environment) {
        this.configuration = configuration;
        this.environment = environment;
    }

    @Override
    public @Nullable ExecutionResult execute(Executor executor, @NotNull ProgramRunner<?> runner) throws ExecutionException {
        @NotNull ProcessHandler processHandler = new InsidiousProcessHandler();
        this.consoleView = new ConsoleViewImpl(configuration.getProject(), false);
        DefaultExecutionResult executionResult = new DefaultExecutionResult(processHandler);
        return executionResult;
    }


    public void setInitialThread(ThreadReference initialThread) {
        this.initialThread = initialThread;
    }

    public String getRunProfileName() {
        return "insidious run profile";
    }

    public boolean isErrored() {
        return true;
    }

    public CommandSender getCommandSender() {
        return null;
    }

    public ThreadReference getInitialThread() {
        return this.initialThread;
    }

    public void setCommandSender(CommandSender commandSender) {

    }

    public void setErrored(boolean b) {

    }

    public ConsoleView getConsoleView() {
        return consoleView;
    }

    public void setInitialSafeBbCount(Long valueOf) {

    }

    public void setConsoleBuilder(TextConsoleBuilder builder) {
        this.consoleBuilder = builder;
    }
}
