package com.insidious.plugin.extension.thread;

import com.insidious.plugin.extension.connector.InsidiousStackFrameProxy;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.jdi.LocalVariableProxy;
import com.sun.jdi.Type;

public interface InsidiousLocalVariableProxy extends LocalVariableProxy {
    String name();

    String typeName();

    Type getType() throws EvaluateException;

    InsidiousStackFrameProxy getFrame();

    InsidiousLocalVariable getLocalVariable();

    boolean isVisible(InsidiousStackFrameProxy paramInsidiousStackFrameProxy);

    boolean isArgument();
}
