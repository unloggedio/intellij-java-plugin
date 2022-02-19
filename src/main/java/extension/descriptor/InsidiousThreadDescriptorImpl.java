package extension.descriptor;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.ui.tree.ThreadDescriptor;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.intellij.icons.AllIcons;
import com.intellij.xdebugger.XDebugProcess;
import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.ThreadGroupReference;
import com.sun.jdi.ThreadReference;
import extension.DebuggerBundle;
import extension.thread.InsidiousThreadReferenceProxy;
import extension.evaluation.EvaluationContext;
import extension.evaluation.InsidiousNodeDescriptorImpl;

import javax.swing.*;

public class InsidiousThreadDescriptorImpl extends InsidiousNodeDescriptorImpl implements ThreadDescriptor {
    private final InsidiousThreadReferenceProxy myThread;
    private String myName = null;

    private boolean myIsExpandable = true;
    private boolean myIsSuspended = false;
    private boolean myIsCurrent;
    private boolean myIsFrozen;
    private boolean myIsAtBreakpoint;

    public InsidiousThreadDescriptorImpl(InsidiousThreadReferenceProxy thread) {
        this.myThread = thread;
    }


    public String getName() {
        return this.myName;
    }


    protected String calcRepresentation(XDebugProcess process, DescriptorLabelListener labelListener) throws EvaluateException {
        InsidiousThreadReferenceProxy InsidiousThreadReferenceProxy = getThreadReference();
        try {
            this.myName = InsidiousThreadReferenceProxy.getThreadReference().name();
            ThreadGroupReference gr = InsidiousThreadReferenceProxy.getThreadReference().threadGroup();
            String grname = (gr != null) ? gr.name() : null;

            String threadStatusText = DebuggerUtilsEx.getThreadStatusText(
                    getThreadReference().getThreadReference().status());

            if (grname != null && !"SYSTEM".equalsIgnoreCase(grname)) {
                return DebuggerBundle.message("label.thread.node.in.group", new Object[]{this.myName,


                        Long.valueOf(InsidiousThreadReferenceProxy.getThreadReference().uniqueID()), threadStatusText, grname});
            }


            return DebuggerBundle.message("label.thread.node", new Object[]{this.myName,


                    Long.valueOf(InsidiousThreadReferenceProxy.getThreadReference().uniqueID()), threadStatusText});
        } catch (ObjectCollectedException e) {
            return (this.myName != null) ?
                    DebuggerBundle.message("label.thread.node.thread.collected", new Object[]{this.myName
                    }) : "";
        }
    }


    public InsidiousThreadReferenceProxy getThreadReference() {
        return this.myThread;
    }

    public boolean isCurrent() {
        return this.myIsCurrent;
    }

    public boolean isFrozen() {
        return this.myIsFrozen;
    }


    public boolean isExpandable() {
        return this.myIsExpandable;
    }


    public void setContext(EvaluationContext context) {
        ThreadReference thread = getThreadReference().getThreadReference();

        try {
            this.myIsSuspended = thread.isSuspended();
        } catch (ObjectCollectedException e) {
            this.myIsSuspended = false;
        }
        this.myIsExpandable = calcExpandable(this.myIsSuspended);
        this.myIsAtBreakpoint = thread.isAtBreakpoint();
        this
                .myIsCurrent = context != null && ((context.getStackFrameProxy().threadProxy() == thread));
        this.myIsFrozen = this.myIsSuspended;
    }

    private boolean calcExpandable(boolean isSuspended) {
        if (!isSuspended) {
            return false;
        }
        int status = getThreadReference().getThreadReference().status();
      return status != -1 && status != 5 && status != 0;
    }


    public boolean isAtBreakpoint() {
        return this.myIsAtBreakpoint;
    }

    public boolean isSuspended() {
        return this.myIsSuspended;
    }

    public Icon getIcon() {
        if (isCurrent()) {
            return AllIcons.Debugger.ThreadCurrent;
        }
        if (isAtBreakpoint()) {
            return AllIcons.Debugger.ThreadAtBreakpoint;
        }
        if (isFrozen()) {
            return AllIcons.Debugger.ThreadFrozen;
        }
        if (isSuspended()) {
            return AllIcons.Debugger.ThreadSuspended;
        }
        return AllIcons.Debugger.ThreadRunning;
    }
}

