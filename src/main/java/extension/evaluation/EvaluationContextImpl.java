package extension.evaluation;


import com.intellij.debugger.EvaluatingComputable;
import com.intellij.debugger.engine.evaluation.DebuggerComputableValue;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.sun.jdi.Value;
import extension.InsidiousStackFrameProxy;
import extension.InsidiousVirtualMachineProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public final class EvaluationContextImpl implements EvaluationContext {
    private static final Logger logger = Logger.getInstance(EvaluationContextImpl.class);

    private final InsidiousStackFrameProxy myStackFrameProxy;

    private final DebuggerComputableValue myThisObject;

    private final InsidiousVirtualMachineProxy myVirtualMachineProxy;

    private final XSuspendContext mySuspendContext;

    private boolean myAutoLoadClasses = true;

    private EvaluationContextImpl(@Nullable XSuspendContext suspendContext, @NotNull InsidiousStackFrameProxy stackFrameProxy, @NotNull DebuggerComputableValue thisObjectComputableValue) {
        this.mySuspendContext = suspendContext;
        this.myVirtualMachineProxy = stackFrameProxy.threadProxy().getVirtualMachine();
        this.myStackFrameProxy = stackFrameProxy;
        this.myThisObject = thisObjectComputableValue;
    }


    public EvaluationContextImpl(@Nullable XSuspendContext suspendContext, @NotNull InsidiousStackFrameProxy stackFrameProxy, @NotNull EvaluatingComputable<? extends Value> thisObjectFactory) {
        this(suspendContext, stackFrameProxy, new DebuggerComputableValue(thisObjectFactory));
    }


    public EvaluationContextImpl(@Nullable XSuspendContext suspendContext, @NotNull InsidiousStackFrameProxy stackFrameProxy, @Nullable Value thisObject) {
        this(suspendContext, stackFrameProxy, new DebuggerComputableValue(thisObject));
    }


    @Nullable
    public Value computeThisObject() throws EvaluateException {
        return this.myThisObject.getValue();
    }


    public XSuspendContext getXSuspendContext() {
        return this.mySuspendContext;
    }


    public EvaluationContextImpl createEvaluationContext(Value value) {
        EvaluationContextImpl copy = new EvaluationContextImpl(this.mySuspendContext, this.myStackFrameProxy, value);

        copy.setAutoLoadClasses(this.myAutoLoadClasses);
        return copy;
    }

    @NotNull
    public InsidiousStackFrameProxy getStackFrameProxy() {
        return this.myStackFrameProxy;
    }

    @NotNull
    public InsidiousVirtualMachineProxy getVirtualMachineProxy() {
        return this.myVirtualMachineProxy;
    }

    public boolean isAutoLoadClasses() {
        return this.myAutoLoadClasses;
    }

    public void setAutoLoadClasses(boolean autoLoadClasses) {
        this.myAutoLoadClasses = autoLoadClasses;
    }

    public EvaluationContextImpl withAutoLoadClasses(boolean autoLoadClasses) {
        if (this.myAutoLoadClasses == autoLoadClasses) {
            return this;
        }
        EvaluationContextImpl copy = new EvaluationContextImpl(this.mySuspendContext, this.myStackFrameProxy, this.myThisObject);

        copy.setAutoLoadClasses(autoLoadClasses);
        return copy;
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\undo-intellij-6.6.1\!\i\\undo\intellij\debugger\evaluation\EvaluationContextImpl.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */