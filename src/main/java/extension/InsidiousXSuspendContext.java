package extension;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.EventSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class InsidiousXSuspendContext extends XSuspendContext {


    private static final Logger logger = Logger.getInstance(InsidiousXSuspendContext.class);
    private final boolean isInternalEvent = false;
    private final InsidiousThreadReferenceProxy thread;
    private final EventSet myEventSet;
    private final int suspendPolicy;
    private final boolean isBookmarkEvent;
    InsidiousDebugProcess debugProcess;
    InsidiousXExecutionStack activeExecutionStack;
    List<InsidiousXExecutionStack> allExecutionStacks;

    public InsidiousXSuspendContext(InsidiousDebugProcess debugProcess, ThreadReference thread, int suspendPolicy) {
        this(debugProcess, thread, suspendPolicy, null, false);
    }


    public InsidiousXSuspendContext(InsidiousDebugProcess debugProcess, ThreadReference thread, int suspendPolicy, boolean isBookmarkEvent) {
        this(debugProcess, thread, suspendPolicy, null, isBookmarkEvent);
    }


    public InsidiousXSuspendContext(InsidiousDebugProcess debugProcess, ThreadReference thread, int suspendPolicy, EventSet event) {
        this(debugProcess, thread, suspendPolicy, event, false);
    }


    public InsidiousXSuspendContext(InsidiousDebugProcess debugProcess, ThreadReference thread, int suspendPolicy, EventSet event, boolean isBookmarkEvent) {
        this.debugProcess = debugProcess;


        ThreadReference newThread = debugProcess.getConnector().getThreadReferenceWithUniqueId((int) thread.uniqueID());
        if (newThread == null) {
            newThread = thread;
        }
        this.thread = new InsidiousThreadReferenceProxyImpl(
                (InsidiousVirtualMachineProxy) debugProcess.getConnector(), newThread);
        this.suspendPolicy = suspendPolicy;
        this.myEventSet = event;
        this.isBookmarkEvent = isBookmarkEvent;
    }

    @Override
    public @Nullable XExecutionStack getActiveExecutionStack() {
        if (this.activeExecutionStack == null) {
            this.activeExecutionStack = new InsidiousXExecutionStack(this, this.thread, this.debugProcess, true);
        }
        return this.activeExecutionStack;
    }

    @Override
    public XExecutionStack @NotNull [] getExecutionStacks() {
        return super.getExecutionStacks();
    }
}
