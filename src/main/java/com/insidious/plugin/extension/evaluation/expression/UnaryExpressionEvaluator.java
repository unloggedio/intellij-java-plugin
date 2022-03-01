package com.insidious.plugin.extension.evaluation.expression;

import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.tree.IElementType;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.PrimitiveValue;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.insidious.plugin.extension.DebuggerBundle;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import com.insidious.plugin.extension.evaluation.EvaluatorUtil;


class UnaryExpressionEvaluator
        implements Evaluator {
    private final IElementType myOperationType;
    private final String myExpectedType;
    private final Evaluator myOperandEvaluator;
    private final String myOperationText;

    UnaryExpressionEvaluator(IElementType operationType, String expectedType, Evaluator operandEvaluator, String operationText) {
        this.myOperationType = operationType;
        this.myExpectedType = expectedType;
        this.myOperandEvaluator = operandEvaluator;
        this.myOperationText = operationText;
    }


    public Object evaluate(EvaluationContext context) throws EvaluateException {
        Value operand = (Value) this.myOperandEvaluator.evaluate(context);
        VirtualMachine vm = context.getVirtualMachineProxy().getVirtualMachine();
        if (this.myOperationType == JavaTokenType.PLUS) {
            if (DebuggerUtils.isNumeric(operand)) {
                return operand;
            }
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.numeric.expected"));
        }
        if (this.myOperationType == JavaTokenType.MINUS) {
            if (DebuggerUtils.isInteger(operand)) {
                long v = ((PrimitiveValue) operand).longValue();
                return EvaluatorUtil.createValue(vm, this.myExpectedType, -v);
            }
            if (DebuggerUtils.isNumeric(operand)) {
                double v = ((PrimitiveValue) operand).doubleValue();
                return EvaluatorUtil.createValue(vm, this.myExpectedType, -v);
            }
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.numeric.expected"));
        }
        if (this.myOperationType == JavaTokenType.TILDE) {
            if (DebuggerUtils.isInteger(operand)) {
                long v = ((PrimitiveValue) operand).longValue();
                return EvaluatorUtil.createValue(vm, this.myExpectedType, v ^ 0xFFFFFFFFFFFFFFFFL);
            }
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.integer.expected"));
        }
        if (this.myOperationType == JavaTokenType.EXCL) {
            if (operand instanceof BooleanValue) {
                boolean v = ((BooleanValue) operand).booleanValue();
                return EvaluatorUtil.createValue(vm, this.myExpectedType, !v);
            }
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.boolean.expected"));
        }

        throw EvaluateExceptionUtil.createEvaluateException(
                DebuggerBundle.message("evaluation.error.operation.not.supported", this.myOperationText));
    }
}


