package com.insidious.plugin.extension.evaluation;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.insidious.plugin.extension.thread.InsidiousVirtualMachineProxy;

public interface JVMName {
    String getName(InsidiousVirtualMachineProxy paramInsidiousVirtualMachineProxy) throws EvaluateException;

    String getDisplayName(InsidiousVirtualMachineProxy paramInsidiousVirtualMachineProxy);
}
