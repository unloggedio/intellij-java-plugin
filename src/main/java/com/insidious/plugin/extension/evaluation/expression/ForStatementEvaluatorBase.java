package com.insidious.plugin.extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.sun.jdi.BooleanValue;
import com.insidious.plugin.extension.evaluation.EvaluationContext;

public abstract class ForStatementEvaluatorBase
        extends LoopEvaluator {
    public ForStatementEvaluatorBase(String labelName, Evaluator bodyEvaluator) {
        super(labelName, bodyEvaluator);
    }


    public Object evaluate(EvaluationContext context) throws EvaluateException {
        Object value = context.getVirtualMachineProxy().getVirtualMachine().mirrorOfVoid();
        value = evaluateInitialization(context, value);


        while (true) {
            Object codition = evaluateCondition(context);
            if (codition instanceof Boolean) {
                if (!((Boolean) codition).booleanValue())
                    break;
            } else if (codition instanceof BooleanValue) {
                if (!((BooleanValue) codition).booleanValue())
                    break;
            } else {
                throw EvaluateExceptionUtil.BOOLEAN_EXPECTED;
            }


            if (body(context)) {
                break;
            }
            value = evaluateUpdate(context, value);
        }

        return value;
    }


    protected Object evaluateInitialization(EvaluationContext context, Object value) throws EvaluateException {
        return value;
    }

    protected Object evaluateCondition(EvaluationContext context) throws EvaluateException {
        return Boolean.valueOf(true);
    }


    protected Object evaluateUpdate(EvaluationContext context, Object value) throws EvaluateException {
        return value;
    }
}


