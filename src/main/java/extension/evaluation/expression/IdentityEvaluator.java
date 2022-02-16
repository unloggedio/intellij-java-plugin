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


