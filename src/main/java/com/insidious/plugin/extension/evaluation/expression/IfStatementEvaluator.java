package com.insidious.plugin.extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.engine.evaluation.expression.Modifier;
import com.sun.jdi.BooleanValue;
import com.insidious.plugin.extension.evaluation.EvaluationContext;


public class IfStatementEvaluator
        implements Evaluator {
    private final Evaluator myConditionEvaluator;
    private final Evaluator myThenEvaluator;
    private final Evaluator myElseEvaluator;
    private Modifier myModifier;

    public IfStatementEvaluator(Evaluator conditionEvaluator, Evaluator thenEvaluator, Evaluator elseEvaluator) {
        this.myConditionEvaluator = DisableGC.create(conditionEvaluator);
        this.myThenEvaluator = DisableGC.create(thenEvaluator);
        this.myElseEvaluator = (elseEvaluator == null) ? null : DisableGC.create(elseEvaluator);
    }


    public Modifier getModifier() {
        return this.myModifier;
    }


    public Object evaluate(EvaluationContext context) throws EvaluateException {
        Object value = this.myConditionEvaluator.evaluate(context);
        if (!(value instanceof BooleanValue)) {
            throw EvaluateExceptionUtil.BOOLEAN_EXPECTED;
        }
        if (((BooleanValue) value).booleanValue()) {
            value = this.myThenEvaluator.evaluate(context);
            this.myModifier = this.myThenEvaluator.getModifier();
        } else if (this.myElseEvaluator != null) {
            value = this.myElseEvaluator.evaluate(context);
            this.myModifier = this.myElseEvaluator.getModifier();
        } else {

            value = context.getVirtualMachineProxy().getVirtualMachine().mirrorOfVoid();
            this.myModifier = null;
        }


        return value;
    }
}


