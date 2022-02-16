package extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.expression.Modifier;
import extension.evaluation.EvaluationContext;


public class PostfixOperationEvaluator
        implements Evaluator {
    private final Evaluator myOperandEvaluator;
    private final Evaluator myIncrementImpl;
    private Modifier myModifier;

    public PostfixOperationEvaluator(Evaluator operandEvaluator, Evaluator incrementImpl) {
        this.myOperandEvaluator = DisableGC.create(operandEvaluator);
        this.myIncrementImpl = DisableGC.create(incrementImpl);
    }


    public Object evaluate(EvaluationContext context) throws EvaluateException {
        Object value = this.myOperandEvaluator.evaluate(context);
        this.myModifier = this.myOperandEvaluator.getModifier();
        Object operationResult = this.myIncrementImpl.evaluate(context);
        AssignmentEvaluator.assign(this.myModifier, operationResult, context);
        return value;
    }


    public Modifier getModifier() {
        return this.myModifier;
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\insidious-intellij-6.6.1\!\i\\insidious\intellij\debugger\evaluation\expression\PostfixOperationEvaluator.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */