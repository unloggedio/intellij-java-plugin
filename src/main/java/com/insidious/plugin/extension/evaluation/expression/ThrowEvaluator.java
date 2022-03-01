package com.insidious.plugin.extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.sun.jdi.ObjectReference;
import com.insidious.plugin.extension.DebuggerBundle;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import org.jetbrains.annotations.NotNull;

public class ThrowEvaluator
        implements Evaluator {
    @NotNull
    private final Evaluator myExceptionEvaluator;

    public ThrowEvaluator(@NotNull Evaluator exceptionEvaluator) {
        this.myExceptionEvaluator = exceptionEvaluator;
    }


    public Object evaluate(EvaluationContext context) throws EvaluateException {
        ObjectReference exception = (ObjectReference) this.myExceptionEvaluator.evaluate(context);
        EvaluateException ex = new EvaluateException(DebuggerBundle.message("evaluation.error.method.exception", exception.referenceType().name()));
        ex.setTargetException(exception);
        throw ex;
    }
}


