package extension;

import com.intellij.debugger.NoDataException;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadGroupReference;
import com.sun.jdi.ThreadReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class InsidiousXExecutionStack extends XExecutionStack {
    private static final Logger logger = Logger.getInstance(InsidiousXExecutionStack.class);
    private final InsidiousXSuspendContext suspendContext;
    private final InsidiousThreadReferenceProxy threadProxy;
    private final InsidiousDebugProcess debugProcess;
    private InsidiousStackFrameProxy myTopStackFrameProxy;
    private XStackFrame topFrame;

    protected InsidiousXExecutionStack(@NotNull InsidiousXSuspendContext suspendContext,
                                       @NotNull InsidiousThreadReferenceProxy threadProxy,
                                       @NotNull InsidiousDebugProcess debugProcess,
                                       boolean current) {
        super(calcRepresentation(threadProxy.getThreadReference()), calcIcon(threadProxy.getThreadReference(), current));


        this.suspendContext = suspendContext;
        this.threadProxy = threadProxy;
        this.debugProcess = debugProcess;
        try {
            if (this.threadProxy.frameCount() > 0) {
                this.myTopStackFrameProxy = new InsidiousStackFrameProxyImpl(this.threadProxy, 0);
                this.topFrame = createXStackFrame(this.myTopStackFrameProxy.getStackFrame(), 0);
            }
        } catch (Exception e) {
            logger.info(e);
        }

    }

    private static Icon calcIcon(ThreadReference thread, boolean current) {
        try {
            if (current)
                return thread.isSuspended() ?
                        AllIcons.Debugger.ThreadCurrent :
                        AllIcons.Debugger.ThreadRunning;
            if (thread.frameCount() > 0 && thread.isAtBreakpoint())
                return AllIcons.Debugger.ThreadAtBreakpoint;
            if (thread.isSuspended()) {
                return AllIcons.Debugger.ThreadSuspended;
            }
            return AllIcons.Debugger.ThreadRunning;
        } catch (Exception ex) {
            return AllIcons.Debugger.Db_obsolete;
        }
    }

    private static String calcRepresentation(ThreadReference thread) {
        String name = "";
        try {
            name = thread.name();
            ThreadGroupReference gr = thread.threadGroup();
            String grname = (gr != null) ? gr.name() : null;
            String threadStatusText = DebuggerUtilsEx.getThreadStatusText(thread.status());

            if (grname != null && !"SYSTEM".equalsIgnoreCase(grname)) {


                name = "\"" + name + "\"@" + thread.uniqueID() + " in group \"" + grname + "\": " + threadStatusText;

            } else {


                name = "\"" + name + "\"@" + thread.uniqueID() + ": " + threadStatusText;
            }
        } catch (Exception exception) {
        }

        return name;
    }

    private XStackFrame createXStackFrame(StackFrame stackFrame, int index) throws NoDataException {
        XSourcePosition xSourcePosition = ReadAction.compute(() -> {
            SourcePosition position = this.debugProcess.getPositionManager().getSourcePosition(stackFrame.location());


            return DebuggerUtilsEx.toXSourcePosition(position);
        });


        InsidiousStackFrameProxyImpl insidiousStackFrameProxy = new InsidiousStackFrameProxyImpl(this.threadProxy, index);
        return new InsidiousXStackFrame(this.debugProcess, this.suspendContext, insidiousStackFrameProxy, xSourcePosition);
    }

    @Override
    public @Nullable XStackFrame getTopFrame() {
        return null;
    }

    @Override
    public void computeStackFrames(int firstFrameIndex, XStackFrameContainer container) {

    }
}
