package com.insidious.plugin.extension.evaluation.expression;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.impl.DebuggerUtilsImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;
import com.insidious.plugin.extension.DebuggerBundle;
import com.insidious.plugin.extension.evaluation.EvaluationContext;


class InstanceofEvaluator
        implements Evaluator {
    private static final Logger logger = LoggerUtil.getInstance(InstanceofEvaluator.class);
    private final Evaluator myOperandEvaluator;
    private final TypeEvaluator myTypeEvaluator;

    InstanceofEvaluator(Evaluator operandEvaluator, TypeEvaluator typeEvaluator) {
        this.myOperandEvaluator = operandEvaluator;
        this.myTypeEvaluator = typeEvaluator;
    }


    public Object evaluate(EvaluationContext context) throws EvaluateException {
        Value value = (Value) this.myOperandEvaluator.evaluate(context);
        if (value == null) {
            return context.getVirtualMachineProxy().getVirtualMachine().mirrorOf(false);
        }
        if (!(value instanceof ObjectReference)) {
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.object.reference.expected"));
        }
        try {
            return context.getVirtualMachineProxy()
                    .getVirtualMachine()
                    .mirrorOf(
                            DebuggerUtilsImpl.instanceOf(((ObjectReference) value)
                                    .referenceType(), this.myTypeEvaluator
                                    .evaluate(context)));
        } catch (Exception e) {
            logger.debug("failed", e);
            throw EvaluateExceptionUtil.createEvaluateException(e);
        }
    }


    public String toString() {
        return this.myOperandEvaluator + " instanceof " + this.myTypeEvaluator;
    }
}


