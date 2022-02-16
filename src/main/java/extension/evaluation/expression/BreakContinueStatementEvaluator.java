package extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.expression.BreakException;
import com.intellij.debugger.engine.evaluation.expression.ContinueException;
import extension.evaluation.EvaluationContext;


public final class BreakContinueStatementEvaluator {
    public static Evaluator createBreakEvaluator(final String labelName) {
        return new Evaluator() {
            public Object evaluate(EvaluationContext context) throws BreakException {
                throw new BreakException(labelName);
            }
        };
    }

    public static Evaluator createContinueEvaluator(final String labelName) {
        return new Evaluator() {
            public Object evaluate(EvaluationContext context) throws ContinueException {
                throw new ContinueException(labelName);
            }
        };
    }
}


