package extension.descriptor;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebuggerBundle;
import extension.DebuggerBundle;
import extension.evaluation.EvaluationContext;
import extension.evaluation.InsidiousNodeDescriptorImpl;

public class InsidiousMessageDescriptor
        extends InsidiousNodeDescriptorImpl {
    public static final int ERROR = 0;
    public static final int WARNING = 1;
    public static final int INFORMATION = 2;
    public static final int SPECIAL = 3;
    public static final InsidiousMessageDescriptor DEBUG_INFO_UNAVAILABLE = new InsidiousMessageDescriptor(

            DebuggerBundle.message("message.node.debug.info.not.available"));
    public static final InsidiousMessageDescriptor LOCAL_VARIABLES_INFO_UNAVAILABLE = new InsidiousMessageDescriptor(

            DebuggerBundle.message("message.node.local.variables.debug.info.not.available"));
    public static final InsidiousMessageDescriptor ARRAY_IS_EMPTY = new InsidiousMessageDescriptor(
            DebuggerBundle.message("message.node.empty.array"));
    public static final InsidiousMessageDescriptor CLASS_HAS_NO_FIELDS = new InsidiousMessageDescriptor(
            DebuggerBundle.message("message.node.class.has.no.fields"));
    public static final InsidiousMessageDescriptor OBJECT_COLLECTED = new InsidiousMessageDescriptor(
            DebuggerBundle.message("message.node.object.collected"));
    public static final InsidiousMessageDescriptor EVALUATING = new InsidiousMessageDescriptor(

            XDebuggerBundle.message("xdebugger.building.tree.node.message"));
    public static final InsidiousMessageDescriptor THREAD_IS_RUNNING = new InsidiousMessageDescriptor(
            DebuggerBundle.message("message.node.thread.running"));
    public static final InsidiousMessageDescriptor THREAD_IS_EMPTY = new InsidiousMessageDescriptor(
            DebuggerBundle.message("message.node.thread.has.no.frames"));
    public static final InsidiousMessageDescriptor EVALUATION_NOT_POSSIBLE = new InsidiousMessageDescriptor(

            DebuggerBundle.message("message.node.evaluation.not.possible", Integer.valueOf(1)));
    private final int myKind;
    private final String myMessage;

    public InsidiousMessageDescriptor(String message) {
        this(message, 2);
    }

    public InsidiousMessageDescriptor(String message, int kind) {
        this.myKind = kind;
        this.myMessage = message;
    }

    public int getKind() {
        return this.myKind;
    }


    public String getLabel() {
        return this.myMessage;
    }


    public boolean isExpandable() {
        return false;
    }


    public void setContext(EvaluationContext context) {
    }


    protected String calcRepresentation(XDebugProcess process, DescriptorLabelListener labelListener) throws EvaluateException {
        return this.myMessage;
    }
}

