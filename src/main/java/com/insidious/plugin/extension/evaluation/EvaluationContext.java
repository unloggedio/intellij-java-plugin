package com.insidious.plugin.extension.evaluation;


import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.sun.jdi.Value;
import com.insidious.plugin.extension.connector.InsidiousStackFrameProxy;
import com.insidious.plugin.extension.thread.InsidiousVirtualMachineProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public interface EvaluationContext {
    EvaluationContext createEvaluationContext(Value paramValue);

    @NotNull
    InsidiousStackFrameProxy getStackFrameProxy();

    @NotNull
    InsidiousVirtualMachineProxy getVirtualMachineProxy();

    @Nullable
    Value computeThisObject() throws EvaluateException;

    XSuspendContext getXSuspendContext();

    default boolean isAutoLoadClasses() {
        return true;
    }
}


