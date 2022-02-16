package extension;

import com.intellij.execution.ExecutionResult;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.EventDispatcher;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import extension.connector.InsidiousConnector;
import extension.connector.InsidiousThreadsDebuggerTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.debugger.JavaDebuggerEditorsProvider;

public class InsidiousDebugProcess extends XDebugProcess {

    private final static Logger logger = Logger.getInstance(InsidiousDebugProcess.class);
    protected final EventDispatcher<InsidiousDebugProcessListener> myDebugProcessDispatcher = EventDispatcher.create(InsidiousDebugProcessListener.class);
    private final JavaDebuggerEditorsProvider editorsProvider;
    private ExecutionResult executionResult;
    private InsidiousCompoundPositionManager myPositionManager;
    private InsidiousConnector connector;
    private InsidiousDebuggerEventThread eventReaderThread;

    /**
     * @param session pass {@code session} parameter of {@link XDebugProcessStarter#start} method to this constructor
     */
    protected InsidiousDebugProcess(@NotNull XDebugSession session) {
        super(session);
        this.editorsProvider = new JavaDebuggerEditorsProvider();
        this.connector = new InsidiousConnector(this);
        InsidiousThreadsDebuggerTree tree = new InsidiousThreadsDebuggerTree(session.getProject(), this);
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


    public void attachVM() {
        // connect to vm
        // this.connector.connect() ?
        this.eventReaderThread = new InsidiousDebuggerEventThread(this);
        this.myDebugProcessDispatcher.getMulticaster().processAttached(this);
        logger.info("Insidious VM started");

    }
}
