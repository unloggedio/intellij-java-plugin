package com.insidious.plugin.extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.expression.Modifier;
import com.sun.jdi.Value;
import com.insidious.plugin.extension.evaluation.EvaluationContext;

public interface ExpressionEvaluator {
    Modifier getModifier();

    Value evaluate(EvaluationContext paramEvaluationContext) throws EvaluateException;
}
