package extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.impl.DebuggerUtilsImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;
import extension.DebuggerBundle;
import extension.evaluation.EvaluationContext;


class InstanceofEvaluator
        implements Evaluator {
    private static final Logger LOG = Logger.getInstance(InstanceofEvaluator.class);
    private final Evaluator myOperandEvaluator;
    private final TypeEvaluator myTypeEvaluator;

    InstanceofEvaluator(Evaluator operandEvaluator, TypeEvaluator typeEvaluator) {
        this.myOperandEvaluator = operandEvaluator;
        this.myTypeEvaluator = typeEvaluator;
    }


    public Object evaluate(EvaluationContext context) throws EvaluateException {
        Value value = (Value) this.myOperandEvaluator.evaluate(context);
        if (value == null) {
            return context.getVirtualMachineProxy().getVirtualMachine().mirrorOf(false);
        }
        if (!(value instanceof ObjectReference)) {
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.object.reference.expected"));
        }
        try {
            return context.getVirtualMachineProxy()
                    .getVirtualMachine()
                    .mirrorOf(
                            DebuggerUtilsImpl.instanceOf(((ObjectReference) value)
                                    .referenceType(), this.myTypeEvaluator
                                    .evaluate(context)));
        } catch (Exception e) {
            LOG.debug(e);
            throw EvaluateExceptionUtil.createEvaluateException(e);
        }
    }


    public String toString() {
        return this.myOperandEvaluator + " instanceof " + this.myTypeEvaluator;
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\evaluation\expression\InstanceofEvaluator.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */