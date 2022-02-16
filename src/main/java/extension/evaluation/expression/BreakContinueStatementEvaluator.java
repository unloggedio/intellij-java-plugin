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


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\evaluation\expression\BreakContinueStatementEvaluator.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */