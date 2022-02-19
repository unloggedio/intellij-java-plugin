package extension.thread;

import com.intellij.xdebugger.XDebugProcess;
import extension.connector.InsidiousStackFrameProxy;
import org.jetbrains.annotations.Nullable;

public interface InsidiousStackFrameContext {
    @Nullable
    InsidiousStackFrameProxy getFrameProxy();

    @Nullable
    XDebugProcess getXDebugProcess();
}


