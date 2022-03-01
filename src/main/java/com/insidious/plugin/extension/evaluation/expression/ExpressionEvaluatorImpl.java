package com.insidious.plugin.extension.evaluation.expression;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.engine.evaluation.expression.Modifier;
import com.intellij.debugger.engine.evaluation.expression.ReturnEvaluator;
import org.slf4j.Logger;
import com.sun.jdi.Value;
import com.insidious.plugin.extension.DebuggerBundle;
import com.insidious.plugin.extension.evaluation.EvaluationContext;


public class ExpressionEvaluatorImpl implements ExpressionEvaluator {
    private static final Logger logger = LoggerUtil.getInstance(ExpressionEvaluator.class);
    private final Evaluator myEvaluator;

    public ExpressionEvaluatorImpl(Evaluator evaluator) {
        this.myEvaluator = evaluator;
    }


    public Modifier getModifier() {
        return this.myEvaluator.getModifier();
    }


    public Value evaluate(EvaluationContext context) throws EvaluateException {
        try {
            if (context.getStackFrameProxy() == null) {
                throw EvaluateExceptionUtil.NULL_STACK_FRAME;
            }

            Object value = this.myEvaluator.evaluate(context);

            if (value != null && !(value instanceof Value)) {
                throw EvaluateExceptionUtil.createEvaluateException(
                        DebuggerBundle.message("evaluation.error.invalid.expression", new Object[]{""}));
            }

            return (Value) value;
        } catch (ReturnEvaluator.ReturnException r) {
            return (Value) r.getReturnValue();
        } catch (Throwable e) {
            logger.debug("failed", e);
            if (e instanceof EvaluateException) {
                throw (EvaluateException) e;
            }
            throw EvaluateExceptionUtil.createEvaluateException(e);
        }
    }
}

