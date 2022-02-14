package extension.evaluation;


import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.sun.jdi.Value;
import extension.InsidiousStackFrameProxy;
import extension.InsidiousVirtualMachineProxy;
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


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\undo-intellij-6.6.1\!\i\\undo\intellij\debugger\evaluation\EvaluationContext.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */