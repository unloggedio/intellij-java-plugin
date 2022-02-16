package extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.expression.Modifier;
import extension.evaluation.EvaluationContext;


public interface Evaluator {
    Object evaluate(EvaluationContext paramEvaluationContext) throws EvaluateException;

    default Modifier getModifier() {
        return null;
    }
}

