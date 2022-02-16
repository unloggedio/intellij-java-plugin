package extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.expression.BreakException;
import com.intellij.debugger.engine.evaluation.expression.ContinueException;
import extension.evaluation.EvaluationContext;

import java.util.Objects;

public abstract class LoopEvaluator
        implements Evaluator {
    private final String myLabelName;
    private final Evaluator myBodyEvaluator;

    public LoopEvaluator(String labelName, Evaluator bodyEvaluator) {
        this.myLabelName = labelName;
        this.myBodyEvaluator = (bodyEvaluator != null) ? DisableGC.create(bodyEvaluator) : null;
    }

    protected boolean body(EvaluationContext context) throws EvaluateException {
        try {
            evaluateBody(context);
        } catch (BreakException e) {
            if (Objects.equals(e.getLabelName(), this.myLabelName)) {
                return true;
            }
            throw e;
        } catch (ContinueException e) {
            if (!Objects.equals(e.getLabelName(), this.myLabelName)) {
                throw e;
            }
        }
        return false;
    }

    public String getLabelName() {
        return this.myLabelName;
    }

    protected void evaluateBody(EvaluationContext context) throws EvaluateException {
        if (this.myBodyEvaluator != null)
            this.myBodyEvaluator.evaluate(context);
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\insidious-intellij-6.6.1\!\i\\insidious\intellij\debugger\evaluation\expression\LoopEvaluator.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */