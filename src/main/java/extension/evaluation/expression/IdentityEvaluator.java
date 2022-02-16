package extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.sun.jdi.Value;
import extension.evaluation.EvaluationContext;


public class IdentityEvaluator
        implements Evaluator {
    private final Value myValue;

    public IdentityEvaluator(Value value) {
        this.myValue = value;
    }


    public Object evaluate(EvaluationContext context) throws EvaluateException {
        return this.myValue;
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\insidious-intellij-6.6.1\!\i\\insidious\intellij\debugger\evaluation\expression\IdentityEvaluator.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */