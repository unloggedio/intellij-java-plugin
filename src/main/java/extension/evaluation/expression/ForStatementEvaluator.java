package extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.expression.Modifier;
import extension.evaluation.EvaluationContext;

public class ForStatementEvaluator
        extends ForStatementEvaluatorBase {
    private final Evaluator myInitializationEvaluator;
    private final Evaluator myConditionEvaluator;
    private final Evaluator myUpdateEvaluator;
    private Modifier myModifier;

    public ForStatementEvaluator(Evaluator initializationEvaluator, Evaluator conditionEvaluator, Evaluator updateEvaluator, Evaluator bodyEvaluator, String labelName) {
        super(labelName, bodyEvaluator);
        this
                .myInitializationEvaluator = (initializationEvaluator != null) ? DisableGC.create(initializationEvaluator) : null;
        this
                .myConditionEvaluator = (conditionEvaluator != null) ? DisableGC.create(conditionEvaluator) : null;
        this.myUpdateEvaluator = (updateEvaluator != null) ? DisableGC.create(updateEvaluator) : null;
    }


    public Modifier getModifier() {
        return this.myModifier;
    }


    protected Object evaluateInitialization(EvaluationContext context, Object value) throws EvaluateException {
        if (this.myInitializationEvaluator != null) {
            value = this.myInitializationEvaluator.evaluate(context);
            this.myModifier = this.myInitializationEvaluator.getModifier();
        }
        return value;
    }


    protected Object evaluateCondition(EvaluationContext context) throws EvaluateException {
        if (this.myConditionEvaluator != null) {
            Object value = this.myConditionEvaluator.evaluate(context);
            this.myModifier = this.myConditionEvaluator.getModifier();
            return value;
        }
        return Boolean.valueOf(true);
    }


    protected Object evaluateUpdate(EvaluationContext context, Object value) throws EvaluateException {
        if (this.myUpdateEvaluator != null) {
            value = this.myUpdateEvaluator.evaluate(context);
            this.myModifier = this.myUpdateEvaluator.getModifier();
        }
        return value;
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\insidious-intellij-6.6.1\!\i\\u
ndo\intellij\debugger\evaluation\expression\ForStatementEvaluator.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */