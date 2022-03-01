package com.insidious.plugin.extension.evaluation;


import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.EvaluatingComputable;
import com.intellij.debugger.engine.evaluation.DebuggerComputableValue;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import org.slf4j.Logger;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.sun.jdi.Value;
import com.insidious.plugin.extension.connector.InsidiousStackFrameProxy;
import com.insidious.plugin.extension.thread.InsidiousVirtualMachineProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public final class EvaluationContextImpl implements EvaluationContext {
    private static final Logger logger = LoggerUtil.getInstance(EvaluationContextImpl.class);

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


