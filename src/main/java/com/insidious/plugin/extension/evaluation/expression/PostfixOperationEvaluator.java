package com.insidious.plugin.extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.expression.Modifier;
import com.insidious.plugin.extension.evaluation.EvaluationContext;


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


