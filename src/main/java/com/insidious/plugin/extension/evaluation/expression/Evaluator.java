package com.insidious.plugin.extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.expression.Modifier;
import com.insidious.plugin.extension.evaluation.EvaluationContext;


public interface Evaluator {
    Object evaluate(EvaluationContext paramEvaluationContext) throws EvaluateException;

    default Modifier getModifier() {
        return null;
    }
}

