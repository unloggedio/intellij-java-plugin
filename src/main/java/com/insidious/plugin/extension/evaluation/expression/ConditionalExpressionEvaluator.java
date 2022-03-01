package com.insidious.plugin.extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.Value;
import com.insidious.plugin.extension.DebuggerBundle;
import com.insidious.plugin.extension.evaluation.EvaluationContext;


class ConditionalExpressionEvaluator
        implements Evaluator {
    private final Evaluator myConditionEvaluator;
    private final Evaluator myThenEvaluator;
    private final Evaluator myElseEvaluator;

    ConditionalExpressionEvaluator(Evaluator conditionEvaluator, Evaluator thenEvaluator, Evaluator elseEvaluator) {
        this.myConditionEvaluator = conditionEvaluator;
        this.myThenEvaluator = thenEvaluator;
        this.myElseEvaluator = elseEvaluator;
    }


    public Object evaluate(EvaluationContext context) throws EvaluateException {
        Value condition = (Value) this.myConditionEvaluator.evaluate(context);
        if (!(condition instanceof BooleanValue)) {
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.boolean.condition.expected"));
        }
        return ((BooleanValue) condition).booleanValue() ?
                this.myThenEvaluator.evaluate(context) :
                this.myElseEvaluator.evaluate(context);
    }
}


