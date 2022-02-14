package extension;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.jdi.LocalVariableProxy;
import com.sun.jdi.Type;

public interface InsidiousLocalVariableProxy extends LocalVariableProxy {
    String name();

    String typeName();

    Type getType() throws EvaluateException;

    InsidiousStackFrameProxy getFrame();

    boolean isVisible(InsidiousStackFrameProxy paramUndoStackFrameProxy);

    boolean isArgument();
}
