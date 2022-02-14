package extension;

import com.intellij.execution.ExecutionResult;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InsidiousDebugProcess extends XDebugProcess {

    private ExecutionResult executionResult;
    private InsidiousCompoundPositionManager myPositionManager;
    private InsidiousConnector connector;

    /**
     * @param session pass {@code session} parameter of {@link XDebugProcessStarter#start} method to this constructor
     */
    protected InsidiousDebugProcess(@NotNull XDebugSession session) {
        super(session);
    }

    public static InsidiousDebugProcess create(XDebugSession session) {
        return new InsidiousDebugProcess(session);
    }

    @Override
    public @NotNull XDebuggerEditorsProvider getEditorsProvider() {
        return new InsidiousDebuggerEditorsProvider();
    }

    public InsidiousCompoundPositionManager getPositionManager() {
        return this.myPositionManager;
    }


    @Override
    public void sessionInitialized() {
        super.sessionInitialized();
    }

    @Override
    protected @Nullable ProcessHandler doGetProcessHandler() {
        return this.executionResult != null ? this.executionResult.getProcessHandler() : super.doGetProcessHandler();
    }

    @Override
    public void startPausing() {
        super.startPausing();
        notifySuspended();
    }

    private void notifySuspended() {
        InsidiousApplicationState state = getProcessHandler().getUserData(InsidiousApplicationState.KEY);
    }

    public void setExecutionResult(ExecutionResult executionResult) {
        this.executionResult = executionResult;
    }

    public InsidiousConnector getConnector() {
        return connector;
    }

    public void setConnector(InsidiousConnector connector) {
        this.connector = connector;
    }
}
