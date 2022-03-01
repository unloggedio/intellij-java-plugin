package com.insidious.plugin.extension.evaluation.expression;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import org.slf4j.Logger;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.tree.IElementType;
import com.sun.jdi.*;
import com.insidious.plugin.extension.DebuggerBundle;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import com.insidious.plugin.extension.evaluation.EvaluatorUtil;
import org.jetbrains.annotations.NotNull;

class BinaryExpressionEvaluator implements Evaluator {
    private static final Logger logger = LoggerUtil.getInstance(BinaryExpressionEvaluator.class);

    private final Evaluator myLeftOperand;

    private final Evaluator myRightOperand;

    private final IElementType myOpType;

    private final String myExpectedType;

    BinaryExpressionEvaluator(@NotNull Evaluator leftOperand, @NotNull Evaluator rightOperand, @NotNull IElementType opType, String expectedType) {
        this.myLeftOperand = DisableGC.create(leftOperand);
        this.myRightOperand = DisableGC.create(rightOperand);
        this.myOpType = opType;
        this.myExpectedType = expectedType;
    }

    static Object evaluateOperation(Value leftResult, IElementType opType, Evaluator rightOperand, String expectedType, EvaluationContext context) throws EvaluateException {
        VirtualMachine vm = context.getVirtualMachineProxy().getVirtualMachine();
        if (leftResult instanceof com.sun.jdi.BooleanValue) {
            boolean v1 = ((PrimitiveValue) leftResult).booleanValue();
            if (opType == JavaTokenType.OROR && v1) {
                return EvaluatorUtil.createValue(vm, expectedType, true);
            }
            if (opType == JavaTokenType.ANDAND && !v1) {
                return EvaluatorUtil.createValue(vm, expectedType, false);
            }
        }
        Value rightResult = (Value) rightOperand.evaluate(context);
        if (opType == JavaTokenType.PLUS) {
            if (DebuggerUtils.isInteger(leftResult) && DebuggerUtils.isInteger(rightResult)) {
                long v1 = ((PrimitiveValue) leftResult).longValue();
                long v2 = ((PrimitiveValue) rightResult).longValue();
                return EvaluatorUtil.createValue(vm, expectedType, v1 + v2);
            }
            if (DebuggerUtils.isNumeric(leftResult) && DebuggerUtils.isNumeric(rightResult)) {
                double v1 = ((PrimitiveValue) leftResult).doubleValue();
                double v2 = ((PrimitiveValue) rightResult).doubleValue();
                return EvaluatorUtil.createValue(vm, expectedType, v1 + v2);
            }
            if (leftResult instanceof CharValue && rightResult instanceof CharValue) {
                char v1 = ((CharValue) leftResult).charValue();
                char v2 = ((CharValue) rightResult).charValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 + v2));
            }
            if (leftResult instanceof com.sun.jdi.StringReference || rightResult instanceof com.sun.jdi.StringReference) {
                String v1 = EvaluatorUtil.getValueAsString(context, leftResult);
                String v2 = EvaluatorUtil.getValueAsString(context, rightResult);
                return vm.mirrorOf(v1 + v2);
            }
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.incompatible.types", "+"));
        }
        if (opType == JavaTokenType.MINUS) {
            if (DebuggerUtils.isInteger(leftResult) && DebuggerUtils.isInteger(rightResult)) {
                long v1 = ((PrimitiveValue) leftResult).longValue();
                long v2 = ((PrimitiveValue) rightResult).longValue();
                return EvaluatorUtil.createValue(vm, expectedType, v1 - v2);
            }
            if (DebuggerUtils.isNumeric(leftResult) && DebuggerUtils.isNumeric(rightResult)) {
                double v1 = ((PrimitiveValue) leftResult).doubleValue();
                double v2 = ((PrimitiveValue) rightResult).doubleValue();
                return EvaluatorUtil.createValue(vm, expectedType, v1 - v2);
            }
            if (leftResult instanceof CharValue && rightResult instanceof CharValue) {
                char v1 = ((CharValue) leftResult).charValue();
                char v2 = ((CharValue) rightResult).charValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 - v2));
            }
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.incompatible.types", "-"));
        }
        if (opType == JavaTokenType.ASTERISK) {
            if (DebuggerUtils.isInteger(leftResult) && DebuggerUtils.isInteger(rightResult)) {
                long v1 = ((PrimitiveValue) leftResult).longValue();
                long v2 = ((PrimitiveValue) rightResult).longValue();
                return EvaluatorUtil.createValue(vm, expectedType, v1 * v2);
            }
            if (DebuggerUtils.isNumeric(leftResult) && DebuggerUtils.isNumeric(rightResult)) {
                double v1 = ((PrimitiveValue) leftResult).doubleValue();
                double v2 = ((PrimitiveValue) rightResult).doubleValue();
                return EvaluatorUtil.createValue(vm, expectedType, v1 * v2);
            }
            if (leftResult instanceof CharValue && rightResult instanceof CharValue) {
                char v1 = ((CharValue) leftResult).charValue();
                char v2 = ((CharValue) rightResult).charValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 * v2));
            }
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.incompatible.types", "*"));
        }
        if (opType == JavaTokenType.DIV) {
            if (DebuggerUtils.isInteger(leftResult) && DebuggerUtils.isInteger(rightResult)) {
                long v1 = ((PrimitiveValue) leftResult).longValue();
                long v2 = ((PrimitiveValue) rightResult).longValue();
                return EvaluatorUtil.createValue(vm, expectedType, v1 / v2);
            }
            if (DebuggerUtils.isNumeric(leftResult) && DebuggerUtils.isNumeric(rightResult)) {
                double v1 = ((PrimitiveValue) leftResult).doubleValue();
                double v2 = ((PrimitiveValue) rightResult).doubleValue();
                return EvaluatorUtil.createValue(vm, expectedType, v1 / v2);
            }
            if (leftResult instanceof CharValue && rightResult instanceof CharValue) {
                char v1 = ((CharValue) leftResult).charValue();
                char v2 = ((CharValue) rightResult).charValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 / v2));
            }
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.incompatible.types", "/"));
        }
        if (opType == JavaTokenType.PERC) {
            if (DebuggerUtils.isInteger(leftResult) && DebuggerUtils.isInteger(rightResult)) {
                long v1 = ((PrimitiveValue) leftResult).longValue();
                long v2 = ((PrimitiveValue) rightResult).longValue();
                return EvaluatorUtil.createValue(vm, expectedType, v1 % v2);
            }
            if (DebuggerUtils.isNumeric(leftResult) && DebuggerUtils.isNumeric(rightResult)) {
                double v1 = ((PrimitiveValue) leftResult).doubleValue();
                double v2 = ((PrimitiveValue) rightResult).doubleValue();
                return EvaluatorUtil.createValue(vm, expectedType, v1 % v2);
            }
            if (leftResult instanceof CharValue && rightResult instanceof CharValue) {
                char v1 = ((CharValue) leftResult).charValue();
                char v2 = ((CharValue) rightResult).charValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 % v2));
            }
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.incompatible.types", "%"));
        }
        if (opType == JavaTokenType.LTLT) {
            if (DebuggerUtils.isInteger(leftResult) && DebuggerUtils.isInteger(rightResult)) {
                long v2 = ((PrimitiveValue) rightResult).longValue();
                if (leftResult instanceof ByteValue)
                    return EvaluatorUtil.createValue(vm, expectedType, (((ByteValue) leftResult)
                            .byteValue() << (int) v2));
                if (leftResult instanceof ShortValue)
                    return EvaluatorUtil.createValue(vm, expectedType, (((ShortValue) leftResult)
                            .shortValue() << (int) v2));
                if (leftResult instanceof IntegerValue) {
                    return EvaluatorUtil.createValue(vm, expectedType, (((IntegerValue) leftResult)
                            .intValue() << (int) v2));
                }
                return EvaluatorUtil.createValue(vm, expectedType, ((PrimitiveValue) leftResult)
                        .longValue() << (int) v2);
            }
            if (leftResult instanceof CharValue && rightResult instanceof CharValue) {
                return EvaluatorUtil.createValue(vm, expectedType, (((CharValue) leftResult)


                        .charValue() << ((CharValue) rightResult)
                        .charValue()));
            }
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.incompatible.types", "<<"));
        }
        if (opType == JavaTokenType.GTGT) {
            if (DebuggerUtils.isInteger(leftResult) && DebuggerUtils.isInteger(rightResult)) {
                long v2 = ((PrimitiveValue) rightResult).longValue();
                if (leftResult instanceof ByteValue)
                    return EvaluatorUtil.createValue(vm, expectedType, (((ByteValue) leftResult)
                            .byteValue() >> (int) v2));
                if (leftResult instanceof ShortValue)
                    return EvaluatorUtil.createValue(vm, expectedType, (((ShortValue) leftResult)
                            .shortValue() >> (int) v2));
                if (leftResult instanceof IntegerValue) {
                    return EvaluatorUtil.createValue(vm, expectedType, (((IntegerValue) leftResult)
                            .intValue() >> (int) v2));
                }
                return EvaluatorUtil.createValue(vm, expectedType, ((PrimitiveValue) leftResult)
                        .longValue() >> (int) v2);
            }
            if (leftResult instanceof CharValue && rightResult instanceof CharValue) {
                return EvaluatorUtil.createValue(vm, expectedType, (((CharValue) leftResult)


                        .charValue() >> ((CharValue) rightResult)
                        .charValue()));
            }
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.incompatible.types", ">>"));
        }
        if (opType == JavaTokenType.GTGTGT) {
            if (DebuggerUtils.isInteger(leftResult) && DebuggerUtils.isInteger(rightResult)) {
                long v2 = ((PrimitiveValue) rightResult).longValue();
                if (leftResult instanceof ByteValue)
                    return EvaluatorUtil.createValue(vm, expectedType, (((ByteValue) leftResult)
                            .byteValue() >>> (int) v2));
                if (leftResult instanceof ShortValue)
                    return EvaluatorUtil.createValue(vm, expectedType, (((ShortValue) leftResult)
                            .shortValue() >>> (int) v2));
                if (leftResult instanceof IntegerValue) {
                    return EvaluatorUtil.createValue(vm, expectedType, (((IntegerValue) leftResult)
                            .intValue() >>> (int) v2));
                }
                return EvaluatorUtil.createValue(vm, expectedType, ((PrimitiveValue) leftResult)
                        .longValue() >>> (int) v2);
            }
            if (leftResult instanceof CharValue && rightResult instanceof CharValue) {
                return EvaluatorUtil.createValue(vm, expectedType, (((CharValue) leftResult)


                        .charValue() >>> ((CharValue) rightResult)
                        .charValue()));
            }
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.incompatible.types", ">>>"));
        }
        if (opType == JavaTokenType.AND) {
            if (DebuggerUtils.isInteger(leftResult) && DebuggerUtils.isInteger(rightResult)) {
                long v1 = ((PrimitiveValue) leftResult).longValue();
                long v2 = ((PrimitiveValue) rightResult).longValue();
                return EvaluatorUtil.createValue(vm, expectedType, v1 & v2);
            }
            if (leftResult instanceof CharValue && rightResult instanceof CharValue) {
                char v1 = ((CharValue) leftResult).charValue();
                char v2 = ((CharValue) rightResult).charValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 & v2));
            }
            if (leftResult instanceof com.sun.jdi.BooleanValue && rightResult instanceof com.sun.jdi.BooleanValue) {
                boolean v1 = ((PrimitiveValue) leftResult).booleanValue();
                boolean v2 = ((PrimitiveValue) rightResult).booleanValue();
                return EvaluatorUtil.createValue(vm, expectedType, v1 & v2);
            }
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.incompatible.types", "&"));
        }
        if (opType == JavaTokenType.OR) {
            if (DebuggerUtils.isInteger(leftResult) && DebuggerUtils.isInteger(rightResult)) {
                long v1 = ((PrimitiveValue) leftResult).longValue();
                long v2 = ((PrimitiveValue) rightResult).longValue();
                return EvaluatorUtil.createValue(vm, expectedType, v1 | v2);
            }
            if (leftResult instanceof CharValue && rightResult instanceof CharValue) {
                char v1 = ((CharValue) leftResult).charValue();
                char v2 = ((CharValue) rightResult).charValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 | v2));
            }
            if (leftResult instanceof com.sun.jdi.BooleanValue && rightResult instanceof com.sun.jdi.BooleanValue) {
                boolean v1 = ((PrimitiveValue) leftResult).booleanValue();
                boolean v2 = ((PrimitiveValue) rightResult).booleanValue();
                return EvaluatorUtil.createValue(vm, expectedType, v1 | v2);
            }
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.incompatible.types", "|"));
        }
        if (opType == JavaTokenType.XOR) {
            if (DebuggerUtils.isInteger(leftResult) && DebuggerUtils.isInteger(rightResult)) {
                long v1 = ((PrimitiveValue) leftResult).longValue();
                long v2 = ((PrimitiveValue) rightResult).longValue();
                return EvaluatorUtil.createValue(vm, expectedType, v1 ^ v2);
            }
            if (leftResult instanceof CharValue && rightResult instanceof CharValue) {
                char v1 = ((CharValue) leftResult).charValue();
                char v2 = ((CharValue) rightResult).charValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 ^ v2));
            }
            if (leftResult instanceof com.sun.jdi.BooleanValue && rightResult instanceof com.sun.jdi.BooleanValue) {
                boolean v1 = ((PrimitiveValue) leftResult).booleanValue();
                boolean v2 = ((PrimitiveValue) rightResult).booleanValue();
                return EvaluatorUtil.createValue(vm, expectedType, v1 ^ v2);
            }
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.incompatible.types", "^"));
        }
        if (opType == JavaTokenType.EQEQ) {
            if (leftResult == null && rightResult == null) {
                return EvaluatorUtil.createValue(vm, expectedType, true);
            }
            if (leftResult == null) {
                return EvaluatorUtil.createValue(vm, expectedType, rightResult.equals(null));
            }
            if (rightResult == null) {
                return EvaluatorUtil.createValue(vm, expectedType, leftResult.equals(null));
            }
            if (DebuggerUtils.isInteger(leftResult) && DebuggerUtils.isInteger(rightResult)) {
                long v1 = ((PrimitiveValue) leftResult).longValue();
                long v2 = ((PrimitiveValue) rightResult).longValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 == v2));
            }
            if (DebuggerUtils.isNumeric(leftResult) && DebuggerUtils.isNumeric(rightResult)) {
                double v1 = ((PrimitiveValue) leftResult).doubleValue();
                double v2 = ((PrimitiveValue) rightResult).doubleValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 == v2));
            }
            if (leftResult instanceof com.sun.jdi.BooleanValue && rightResult instanceof com.sun.jdi.BooleanValue) {
                boolean v1 = ((PrimitiveValue) leftResult).booleanValue();
                boolean v2 = ((PrimitiveValue) rightResult).booleanValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 == v2));
            }
            if (leftResult instanceof CharValue && rightResult instanceof CharValue) {
                char v1 = ((CharValue) leftResult).charValue();
                char v2 = ((CharValue) rightResult).charValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 == v2));
            }
            if (leftResult instanceof ObjectReference && rightResult instanceof ObjectReference) {
                ObjectReference v1 = (ObjectReference) leftResult;
                ObjectReference v2 = (ObjectReference) rightResult;
                return EvaluatorUtil.createValue(vm, expectedType, (v1.uniqueID() == v2.uniqueID()));
            }
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.incompatible.types", "=="));
        }
        if (opType == JavaTokenType.OROR) {
            if (leftResult instanceof com.sun.jdi.BooleanValue && rightResult instanceof com.sun.jdi.BooleanValue) {
                boolean v1 = ((PrimitiveValue) leftResult).booleanValue();
                boolean v2 = ((PrimitiveValue) rightResult).booleanValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 || v2));
            }
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.incompatible.types", "||"));
        }
        if (opType == JavaTokenType.ANDAND) {
            if (leftResult instanceof com.sun.jdi.BooleanValue && rightResult instanceof com.sun.jdi.BooleanValue) {
                boolean v1 = ((PrimitiveValue) leftResult).booleanValue();
                boolean v2 = ((PrimitiveValue) rightResult).booleanValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 && v2));
            }
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.incompatible.types", "&&"));
        }
        if (opType == JavaTokenType.NE) {
            if (leftResult == null && rightResult == null)
                return EvaluatorUtil.createValue(vm, expectedType, false);
            if (leftResult == null)
                return EvaluatorUtil.createValue(vm, expectedType, !rightResult.equals(null));
            if (rightResult == null)
                return EvaluatorUtil.createValue(vm, expectedType, !leftResult.equals(null));
            if (DebuggerUtils.isInteger(leftResult) && DebuggerUtils.isInteger(rightResult)) {
                long v1 = ((PrimitiveValue) leftResult).longValue();
                long v2 = ((PrimitiveValue) rightResult).longValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 != v2));
            }
            if (DebuggerUtils.isNumeric(leftResult) && DebuggerUtils.isNumeric(rightResult)) {
                double v1 = ((PrimitiveValue) leftResult).doubleValue();
                double v2 = ((PrimitiveValue) rightResult).doubleValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 != v2));
            }
            if (leftResult instanceof com.sun.jdi.BooleanValue && rightResult instanceof com.sun.jdi.BooleanValue) {
                boolean v1 = ((PrimitiveValue) leftResult).booleanValue();
                boolean v2 = ((PrimitiveValue) rightResult).booleanValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 != v2));
            }
            if (leftResult instanceof CharValue && rightResult instanceof CharValue) {
                char v1 = ((CharValue) leftResult).charValue();
                char v2 = ((CharValue) rightResult).charValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 != v2));
            }
            if (leftResult instanceof ObjectReference && rightResult instanceof ObjectReference) {
                ObjectReference v1 = (ObjectReference) leftResult;
                ObjectReference v2 = (ObjectReference) rightResult;
                return EvaluatorUtil.createValue(vm, expectedType, (v1.uniqueID() != v2.uniqueID()));
            }
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.incompatible.types", "!="));
        }
        if (opType == JavaTokenType.LT) {
            if (DebuggerUtils.isInteger(leftResult) && DebuggerUtils.isInteger(rightResult)) {
                long v1 = ((PrimitiveValue) leftResult).longValue();
                long v2 = ((PrimitiveValue) rightResult).longValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 < v2));
            }
            if (DebuggerUtils.isNumeric(leftResult) && DebuggerUtils.isNumeric(rightResult)) {
                double v1 = ((PrimitiveValue) leftResult).doubleValue();
                double v2 = ((PrimitiveValue) rightResult).doubleValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 < v2));
            }
            if (leftResult instanceof CharValue && rightResult instanceof CharValue) {
                char v1 = ((CharValue) leftResult).charValue();
                char v2 = ((CharValue) rightResult).charValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 < v2));
            }
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.incompatible.types", "<"));
        }
        if (opType == JavaTokenType.GT) {
            if (DebuggerUtils.isInteger(leftResult) && DebuggerUtils.isInteger(rightResult)) {
                long v1 = ((PrimitiveValue) leftResult).longValue();
                long v2 = ((PrimitiveValue) rightResult).longValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 > v2));
            }
            if (DebuggerUtils.isNumeric(leftResult) && DebuggerUtils.isNumeric(rightResult)) {
                double v1 = ((PrimitiveValue) leftResult).doubleValue();
                double v2 = ((PrimitiveValue) rightResult).doubleValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 > v2));
            }
            if (leftResult instanceof CharValue && rightResult instanceof CharValue) {
                char v1 = ((CharValue) leftResult).charValue();
                char v2 = ((CharValue) rightResult).charValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 > v2));
            }
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.incompatible.types", ">"));
        }
        if (opType == JavaTokenType.LE) {
            if (DebuggerUtils.isInteger(leftResult) && DebuggerUtils.isInteger(rightResult)) {
                long v1 = ((PrimitiveValue) leftResult).longValue();
                long v2 = ((PrimitiveValue) rightResult).longValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 <= v2));
            }
            if (DebuggerUtils.isNumeric(leftResult) && DebuggerUtils.isNumeric(rightResult)) {
                double v1 = ((PrimitiveValue) leftResult).doubleValue();
                double v2 = ((PrimitiveValue) rightResult).doubleValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 <= v2));
            }
            if (leftResult instanceof CharValue && rightResult instanceof CharValue) {
                char v1 = ((CharValue) leftResult).charValue();
                char v2 = ((CharValue) rightResult).charValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 <= v2));
            }
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.incompatible.types", "<="));
        }
        if (opType == JavaTokenType.GE) {
            if (DebuggerUtils.isInteger(leftResult) && DebuggerUtils.isInteger(rightResult)) {
                long v1 = ((PrimitiveValue) leftResult).longValue();
                long v2 = ((PrimitiveValue) rightResult).longValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 >= v2));
            }
            if (DebuggerUtils.isNumeric(leftResult) && DebuggerUtils.isNumeric(rightResult)) {
                double v1 = ((PrimitiveValue) leftResult).doubleValue();
                double v2 = ((PrimitiveValue) rightResult).doubleValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 >= v2));
            }
            if (leftResult instanceof CharValue && rightResult instanceof CharValue) {
                char v1 = ((CharValue) leftResult).charValue();
                char v2 = ((CharValue) rightResult).charValue();
                return EvaluatorUtil.createValue(vm, expectedType, (v1 >= v2));
            }
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.incompatible.types", ">="));
        }

        logger.info("assert false");

        return null;
    }

    public Object evaluate(EvaluationContext context) throws EvaluateException {
        Value leftResult = (Value) this.myLeftOperand.evaluate(context);
        return evaluateOperation(leftResult, this.myOpType, this.myRightOperand, this.myExpectedType, context);
    }
}
