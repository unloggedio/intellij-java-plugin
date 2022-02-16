package extension.evaluation.expression;

import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.util.ThreeState;
import com.sun.jdi.*;
import extension.connector.InsidiousStackFrameProxy;
import extension.evaluation.EvaluationContext;
import extension.util.DebuggerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class AssertStatementEvaluator implements Evaluator {
    @NotNull
    private final Evaluator myEvaluator;

    public AssertStatementEvaluator(@NotNull Evaluator evaluator) {
        this.myEvaluator = evaluator;
    }

    public Object evaluate(EvaluationContext context) throws EvaluateException {
        InsidiousStackFrameProxy InsidiousStackFrameProxy = context.getStackFrameProxy();
        if (InsidiousStackFrameProxy == null) {
            throw EvaluateExceptionUtil.NULL_STACK_FRAME;
        }
        ThreeState status = DebuggerUtil.getEffectiveAssertionStatus(InsidiousStackFrameProxy.location());
        if (status == ThreeState.UNSURE) {


            ClassObjectReference classObjectReference = InsidiousStackFrameProxy.location().declaringType().classObject();

            Method method = DebuggerUtils.findMethod(classObjectReference
                    .referenceType(), "desiredAssertionStatus", "()Z");
            if (method != null) {
                ThreadReference thread = InsidiousStackFrameProxy.threadProxy().getThreadReference();
                Value res = null;

                try {
                    res = classObjectReference.invokeMethod(thread, method,


                            Collections.emptyList(), 1);
                } catch (InvalidTypeException | com.sun.jdi.ClassNotLoadedException | com.sun.jdi.IncompatibleThreadStateException | com.sun.jdi.InvocationException e) {


                    e.printStackTrace();
                }
                if (res instanceof BooleanValue) {
                    status = ThreeState.fromBoolean(((BooleanValue) res).value());
                }
            }
        }
        if (status == ThreeState.NO) {
            return InsidiousStackFrameProxy.getStackFrame().virtualMachine().mirrorOfVoid();
        }
        return this.myEvaluator.evaluate(context);
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\evaluation\expression\AssertStatementEvaluator.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */