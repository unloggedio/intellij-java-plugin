package com.insidious.plugin.extension.evaluation.expression;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Couple;
import com.intellij.psi.impl.PsiJavaParserFacadeImpl;
import com.sun.jdi.*;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import com.insidious.plugin.extension.util.DebuggerUtilsAsync;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class UnBoxingEvaluator implements Evaluator {
    private static final Logger logger = LoggerUtil.getInstance(UnBoxingEvaluator.class);
    private static final Map<String, Couple<String>> TYPES_TO_CONVERSION_METHOD_MAP = new HashMap<>();

    static {
        TYPES_TO_CONVERSION_METHOD_MAP.put("java.lang.Boolean",
                Couple.of("booleanValue", "()Z"));
        TYPES_TO_CONVERSION_METHOD_MAP.put("java.lang.Byte",
                Couple.of("byteValue", "()B"));
        TYPES_TO_CONVERSION_METHOD_MAP.put("java.lang.Character",
                Couple.of("charValue", "()C"));
        TYPES_TO_CONVERSION_METHOD_MAP.put("java.lang.Short",
                Couple.of("shortValue", "()S"));
        TYPES_TO_CONVERSION_METHOD_MAP.put("java.lang.Integer",
                Couple.of("intValue", "()I"));
        TYPES_TO_CONVERSION_METHOD_MAP.put("java.lang.Long",
                Couple.of("longValue", "()J"));
        TYPES_TO_CONVERSION_METHOD_MAP.put("java.lang.Float",
                Couple.of("floatValue", "()F"));
        TYPES_TO_CONVERSION_METHOD_MAP.put("java.lang.Double",
                Couple.of("doubleValue", "()D"));
    }

    private final Evaluator myOperand;

    public UnBoxingEvaluator(@NotNull Evaluator operand) {
        this.myOperand = DisableGC.create(operand);
    }

    public static boolean isTypeUnboxable(String typeName) {
        return TYPES_TO_CONVERSION_METHOD_MAP.containsKey(typeName);
    }

    public static Object unbox(@Nullable Object value, EvaluationContext context) throws EvaluateException {
        if (value == null) {
            throw new EvaluateException("java.lang.NullPointerException: cannot unbox null value");
        }
        if (value instanceof ObjectReference) {
            String valueTypeName = ((ObjectReference) value).type().name();
            Couple<String> pair = TYPES_TO_CONVERSION_METHOD_MAP.get(valueTypeName);
            if (pair != null) {
                return convertToPrimitive(context, (ObjectReference) value, pair
                        .getFirst(), pair.getSecond());
            }
        }
        return value;
    }

    private static Value convertToPrimitive(EvaluationContext context, ObjectReference value, String conversionMethodName, String conversionMethodSignature) throws EvaluateException {
        Value primitiveValue = getInnerPrimitiveValue(value, true);
        if (primitiveValue != null) {
            return primitiveValue;
        }


        Method method = DebuggerUtils.findMethod(value
                .referenceType(), conversionMethodName, conversionMethodSignature);
        if (method == null) {
            throw new EvaluateException("Cannot convert to primitive value of type "
                    + value.type()
                    + ": Unable to find method " + conversionMethodName + conversionMethodSignature);
        }


        ThreadReference thread = context.getStackFrameProxy().threadProxy().getThreadReference();
        try {
            return value.invokeMethod(thread, method,


                    Collections.emptyList(), 1);
        } catch (Exception e) {
            throw new EvaluateException(e.getMessage(), e);
        }
    }

    public static PrimitiveValue getInnerPrimitiveValue(@Nullable ObjectReference value, boolean now) {
        if (value != null) {
            ReferenceType type = value.referenceType();
            Field valueField = type.fieldByName("value");
            if (valueField != null) {
                Value primitiveValue = value.getValue(valueField);
                if (primitiveValue instanceof PrimitiveValue) {
                    logger.info("assert type name -" +
                            type.name().equals(PsiJavaParserFacadeImpl.getPrimitiveType(primitiveValue.type().name()).getBoxedTypeName()));
                    return (PrimitiveValue) primitiveValue;
                }
            }
        }
        return null;

    }

    private static CompletableFuture<List<Field>> fields(ReferenceType type, boolean now) {
        return now ? CompletableFuture.completedFuture(type.fields()) : DebuggerUtilsAsync.fields(type);
    }

    private static CompletableFuture<Value> getValue(ObjectReference ref, Field field, boolean now) {
        return now ? CompletableFuture.completedFuture(ref.getValue(field)) : DebuggerUtilsAsync.getValue(ref, field);
    }

    public Object evaluate(EvaluationContext context) throws EvaluateException {
        return unbox(this.myOperand.evaluate(context), context);
    }
}

