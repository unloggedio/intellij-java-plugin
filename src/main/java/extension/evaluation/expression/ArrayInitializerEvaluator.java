package extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import extension.evaluation.EvaluationContext;


class ArrayInitializerEvaluator
        implements Evaluator {
    private final Evaluator[] myValueEvaluators;

    ArrayInitializerEvaluator(Evaluator[] valueEvaluators) {
        this.myValueEvaluators = valueEvaluators;
    }


    public Object evaluate(EvaluationContext context) throws EvaluateException {
        Object[] values = new Object[this.myValueEvaluators.length];
        for (int idx = 0; idx < this.myValueEvaluators.length; idx++) {
            Evaluator evaluator = this.myValueEvaluators[idx];
            values[idx] = evaluator.evaluate(context);
        }
        return values;
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\evaluation\expression\ArrayInitializerEvaluator.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */