package extension;

import com.intellij.xdebugger.XDebugProcess;
import extension.connector.InsidiousStackFrameProxy;
import org.jetbrains.annotations.Nullable;

public interface InsidiousStackFrameContext {
    @Nullable
    InsidiousStackFrameProxy getFrameProxy();

    @Nullable
    XDebugProcess getXDebugProcess();
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\InsidiousStackFrameContext.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */