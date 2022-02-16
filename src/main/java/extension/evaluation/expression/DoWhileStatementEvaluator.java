package extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.engine.evaluation.expression.Modifier;
import com.sun.jdi.BooleanValue;
import extension.evaluation.EvaluationContext;
import org.jetbrains.annotations.NotNull;


public class DoWhileStatementEvaluator
        extends LoopEvaluator {
    private final Evaluator myConditionEvaluator;

    public DoWhileStatementEvaluator(@NotNull Evaluator conditionEvaluator, Evaluator bodyEvaluator, String labelName) {
        super(labelName, bodyEvaluator);
        this.myConditionEvaluator = DisableGC.create(conditionEvaluator);
    }


    public Modifier getModifier() {
        return this.myConditionEvaluator.getModifier();
    }


    public Object evaluate(EvaluationContext context) throws EvaluateException {
        Object value = context.getVirtualMachineProxy().getVirtualMachine().mirrorOfVoid();

        while (!body(context)) {

            value = this.myConditionEvaluator.evaluate(context);
            if (!(value instanceof BooleanValue)) {
                throw EvaluateExceptionUtil.BOOLEAN_EXPECTED;
            }
            if (!((BooleanValue) value).booleanValue()) {
                break;
            }
        }


        return value;
    }
}


