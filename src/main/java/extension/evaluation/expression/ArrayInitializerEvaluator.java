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


