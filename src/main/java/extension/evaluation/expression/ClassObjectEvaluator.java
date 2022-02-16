package extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import extension.evaluation.EvaluationContext;


public class ClassObjectEvaluator
        implements Evaluator {
    private final TypeEvaluator myTypeEvaluator;

    public ClassObjectEvaluator(TypeEvaluator typeEvaluator) {
        this.myTypeEvaluator = typeEvaluator;
    }


    public Object evaluate(EvaluationContext context) throws EvaluateException {
        return this.myTypeEvaluator.evaluate(context).classObject();
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\insidious-intellij-6.6.1\!\i\\insidious\intellij\debugger\evaluation\expression\ClassObjectEvaluator.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */