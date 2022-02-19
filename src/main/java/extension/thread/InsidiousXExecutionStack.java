package extension.thread;

import com.intellij.debugger.NoDataException;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadGroupReference;
import com.sun.jdi.ThreadReference;
import extension.DebuggerBundle;
import extension.InsidiousJavaDebugProcess;
import extension.InsidiousXSuspendContext;
import extension.connector.InsidiousStackFrameProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.Iterator;

public class InsidiousXExecutionStack extends XExecutionStack {
    private static final Logger logger = Logger.getInstance(InsidiousXExecutionStack.class);

    private final InsidiousThreadReferenceProxy myThreadProxy;

    private final InsidiousJavaDebugProcess myDebugProcess;
    private final InsidiousXSuspendContext mySuspendContext;
    private XStackFrame topFrame;
    private InsidiousStackFrameProxy myTopStackFrameProxy;
    private boolean computedFrames = false;

    public InsidiousXExecutionStack(@NotNull InsidiousXSuspendContext suspendContext, @NotNull InsidiousThreadReferenceProxy thread, @NotNull InsidiousJavaDebugProcess debugProcess, boolean current) {
        super(
                calcRepresentation(thread.getThreadReference()),
                calcIcon(thread.getThreadReference(), current));
        this.mySuspendContext = suspendContext;
        this.myThreadProxy = thread;
        this.myDebugProcess = debugProcess;
        try {
            if (this.myThreadProxy.frameCount() > 0) {
                this.myTopStackFrameProxy = new InsidiousStackFrameProxyImpl(this.myThreadProxy, 0);
                this.topFrame = createXStackFrame(this.myTopStackFrameProxy.getStackFrame(), 0);
            }
        } catch (Exception e) {
            logger.info(e);
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

    private XStackFrame createXStackFrame(StackFrame stackFrame, int index) throws NoDataException {
        XSourcePosition xSourcePosition = ReadAction.compute(() -> {
            SourcePosition position = this.myDebugProcess.getPositionManager().getSourcePosition(stackFrame.location());


            return DebuggerUtilsEx.toXSourcePosition(position);
        });


        InsidiousStackFrameProxyImpl InsidiousStackFrameProxyImpl = new InsidiousStackFrameProxyImpl(this.myThreadProxy, index);
        return new InsidiousXStackFrame(this.myDebugProcess, this.mySuspendContext, InsidiousStackFrameProxyImpl, xSourcePosition);
    }

    @Nullable
    public XStackFrame getTopFrame() {
        return this.topFrame;
    }

    public void computeStackFrames(int firstFrameIndex, XExecutionStack.XStackFrameContainer container) {
        if (container.isObsolete())
            return;
        try {
            ThreadReference thread = this.myThreadProxy.getThreadReference();
            int status = thread.status();
            if (status == 0) {
                container.errorOccurred(DebuggerBundle.message("frame.panel.thread.finished"));
            } else if (!thread.isCollected() && thread.isSuspended()) {
                if (status != -1 && status != 5) {

                    try {
                        if (!this.computedFrames) {
                            Iterator<StackFrame> iterator = thread.frames().iterator();
                            int counter = -1;
                            while (iterator.hasNext()) {
                                counter++;
                                StackFrame stackFrame = iterator.next();
                                if (counter >= firstFrameIndex) {
                                    XStackFrame frame = createXStackFrame(stackFrame, counter);
                                    container.addStackFrames(
                                            Collections.singletonList(frame), false);
                                }
                            }
                            this.computedFrames = true;
                        }
                        container.addStackFrames(Collections.emptyList(), true);
                    } catch (IncompatibleThreadStateException | NoDataException e) {
                        container.errorOccurred(e.getMessage());
                    }
                }
            } else {
                container.errorOccurred("Frames not available for unsuspended thread");
            }
        } catch (Exception ex) {
            container.errorOccurred(ex.toString());
        }
    }

    public InsidiousStackFrameProxy getTopStackFrameProxy() {
        return this.myTopStackFrameProxy;
    }
}

