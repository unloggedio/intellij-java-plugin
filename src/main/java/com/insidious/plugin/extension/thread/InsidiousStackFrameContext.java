package com.insidious.plugin.extension.thread;

import com.intellij.xdebugger.XDebugProcess;
import com.insidious.plugin.extension.connector.InsidiousStackFrameProxy;
import org.jetbrains.annotations.Nullable;

public interface InsidiousStackFrameContext {
    @Nullable
    InsidiousStackFrameProxy getFrameProxy();

    @Nullable
    XDebugProcess getXDebugProcess();
}


