package extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import extension.evaluation.EvaluationContext;
import org.jetbrains.annotations.Nullable;

public class ReturnEvaluator
        implements Evaluator {
    @Nullable
    private final Evaluator myReturnValueEvaluator;

    public ReturnEvaluator(@Nullable Evaluator returnValueEvaluator) {
        this.myReturnValueEvaluator = returnValueEvaluator;
    }


    public Object evaluate(EvaluationContext context) throws EvaluateException {
        Object returnValue = (this.myReturnValueEvaluator == null) ? context.getVirtualMachineProxy().getVirtualMachine().mirrorOfVoid() : this.myReturnValueEvaluator.evaluate(context);
        throw new ReturnException(returnValue);
    }

    public static class ReturnException extends EvaluateException {
        private final Object myReturnValue;

        public ReturnException(Object returnValue) {
            super("Return");
            this.myReturnValue = returnValue;
        }

        public Object getReturnValue() {
            return this.myReturnValue;
        }
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\insidious-intellij-6.6.1\!\i\\insidious\intellij\debugger\evaluation\expression\ReturnEvaluator.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */