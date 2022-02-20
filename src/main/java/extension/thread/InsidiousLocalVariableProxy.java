package extension.thread;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.jdi.LocalVariableProxy;
import com.sun.jdi.Type;
import extension.connector.InsidiousStackFrameProxy;

public interface InsidiousLocalVariableProxy extends LocalVariableProxy {
    String name();

    String typeName();

    Type getType() throws EvaluateException;

    InsidiousStackFrameProxy getFrame();

    boolean isVisible(InsidiousStackFrameProxy paramInsidiousStackFrameProxy);

    boolean isArgument();
}
