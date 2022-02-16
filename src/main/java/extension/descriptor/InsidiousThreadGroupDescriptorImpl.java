package extension.descriptor;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.ui.tree.ThreadGroupDescriptor;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.intellij.xdebugger.XDebugProcess;
import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.ThreadGroupReference;
import com.sun.jdi.ThreadReference;
import extension.DebuggerBundle;
import extension.InsidiousThreadGroupReferenceProxy;
import extension.evaluation.EvaluationContext;
import extension.evaluation.InsidiousNodeDescriptorImpl;

public class InsidiousThreadGroupDescriptorImpl extends InsidiousNodeDescriptorImpl implements ThreadGroupDescriptor {
    private final InsidiousThreadGroupReferenceProxy myThreadGroup;
    private boolean myIsCurrent;
    private String myName = null;
    private boolean myIsExpandable = true;

    public InsidiousThreadGroupDescriptorImpl(InsidiousThreadGroupReferenceProxy threadGroup) {
        this.myThreadGroup = threadGroup;
    }


    public InsidiousThreadGroupReferenceProxy getThreadGroupReference() {
        return this.myThreadGroup;
    }

    public boolean isCurrent() {
        return this.myIsCurrent;
    }


    public String getName() {
        return this.myName;
    }


    protected String calcRepresentation(XDebugProcess process, DescriptorLabelListener labelListener) throws EvaluateException {
        ThreadGroupReference group = getThreadGroupReference().getThreadGroupReference();
        try {
            this.myName = group.name();
            return DebuggerBundle.message("label.thread.group.node", this.myName, Long.valueOf(group.uniqueID()));
        } catch (ObjectCollectedException e) {
            return (this.myName != null) ?
                    DebuggerBundle.message("label.thread.group.node.group.collected", this.myName) : "";
        }
    }


    public boolean isExpandable() {
        return this.myIsExpandable;
    }


    public void setContext(EvaluationContext context) {
        ThreadReference thread = context.getStackFrameProxy().threadProxy().getThreadReference();
        this.myIsCurrent = (thread != null && isDescendantGroup(thread.threadGroup()));
        this.myIsExpandable = calcExpandable();
    }

    private boolean isDescendantGroup(ThreadGroupReference group) {
        if (group == null) return false;

        if (getThreadGroupReference() == group) return true;

        return isDescendantGroup(group.parent());
    }

    private boolean calcExpandable() {
        return (this.myThreadGroup.threads().size() > 0 || this.myThreadGroup.threadGroups().size() > 0);
    }
}

