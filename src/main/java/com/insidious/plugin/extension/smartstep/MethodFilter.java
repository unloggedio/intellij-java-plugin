package com.insidious.plugin.extension.smartstep;

import com.insidious.plugin.extension.InsidiousXSuspendContext;
import com.insidious.plugin.extension.connector.InsidiousStackFrameProxy;
import com.insidious.plugin.extension.connector.RequestHint;
import com.insidious.plugin.extension.thread.InsidiousVirtualMachineProxy;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.util.Range;
import com.sun.jdi.Location;
import org.jetbrains.annotations.Nullable;


public interface MethodFilter {
    boolean locationMatches(InsidiousVirtualMachineProxy paramInsidiousVirtualMachineProxy, Location paramLocation) throws EvaluateException;

    default boolean locationMatches(InsidiousVirtualMachineProxy virtualMachineProxy, Location location, @Nullable InsidiousStackFrameProxy frameProxy) throws EvaluateException {
        return locationMatches(virtualMachineProxy, location);
    }

    @Nullable
    Range<Integer> getCallingExpressionLines();

    default int onReached(InsidiousXSuspendContext context, RequestHint hint) {
        return 0;
    }

    default int getSkipCount() {
        return 0;
    }
}


