package com.insidious.plugin.extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.insidious.plugin.extension.evaluation.EvaluationContext;


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

