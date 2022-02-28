package extension;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.util.Key;
import com.sun.jdi.ThreadReference;
import factory.InsidiousService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InsidiousApplicationState implements RunProfileState {

    public static final Key<InsidiousApplicationState> KEY = Key.create(InsidiousApplicationState.class.getName());
    private final InsidiousRunConfiguration configuration;
    private final ExecutionEnvironment environment;
    private ConsoleView consoleView;
    private ThreadReference initialThread;
    private TextConsoleBuilder consoleBuilder;
    private CommandSender commandSender;
    private boolean errored;


    public InsidiousApplicationState(@NotNull InsidiousRunConfiguration configuration, ExecutionEnvironment environment) {
        this.configuration = configuration;
        this.environment = environment;
    }

    @Override
    public @Nullable ExecutionResult execute(Executor executor, @NotNull ProgramRunner<?> runner) throws ExecutionException {
        @NotNull ProcessHandler processHandler = new InsidiousProcessHandler();
        InsidiousService insidiousService = configuration.getProject().getService(InsidiousService.class);
        insidiousService.setProcessHandler(processHandler);

        this.consoleView = new ConsoleViewImpl(configuration.getProject(), false);
        processHandler.putUserData(KEY, this);
        this.consoleView.attachToProcess(processHandler);
        DefaultExecutionResult executionResult = new DefaultExecutionResult(this.consoleView, processHandler);
        return executionResult;
    }

    public String getRunProfileName() {
        return "insidious run profile";
    }

    public boolean isErrored() {
        return this.errored;
    }

    public void setErrored(boolean b) {
        this.errored = b;
    }

    public CommandSender getCommandSender() {
        return this.commandSender;
    }

    public void setCommandSender(CommandSender commandSender) {
        this.commandSender = commandSender;
    }

    public ThreadReference getInitialThread() {
        return this.initialThread;
    }

    public void setInitialThread(ThreadReference initialThread) {
        this.initialThread = initialThread;
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
