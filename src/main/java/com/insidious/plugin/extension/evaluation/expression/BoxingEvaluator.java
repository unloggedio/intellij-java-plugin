package com.insidious.plugin.extension.evaluation.expression;

import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.JVMNameUtil;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.impl.PsiJavaParserFacadeImpl;
import com.sun.jdi.*;
import com.insidious.plugin.extension.evaluation.EvaluationContext;

import java.util.Collections;
import java.util.List;

public class BoxingEvaluator implements Evaluator {
    private final Evaluator myOperand;


    public BoxingEvaluator(Evaluator operand) {
        this.myOperand = DisableGC.create(operand);
    }

    public static Object box(Object value, EvaluationContext context) throws EvaluateException {
        if (value instanceof PrimitiveValue) {
            PrimitiveValue primitiveValue = (PrimitiveValue) value;

            PsiPrimitiveType primitiveType = PsiJavaParserFacadeImpl.getPrimitiveType(primitiveValue.type().name());
            if (primitiveType != null) {
                return convertToWrapper(context, primitiveValue, primitiveType.getBoxedTypeName());
            }
        }
        return value;
    }

    private static Value convertToWrapper(EvaluationContext context, PrimitiveValue value, String wrapperTypeName) throws EvaluateException {
        List<ReferenceType> wrapperClasses = context.getStackFrameProxy().getVirtualMachine().classesByName(wrapperTypeName);
        ClassType wrapperClass = (ClassType) wrapperClasses.get(0);


        String methodSignature = "(" + JVMNameUtil.getPrimitiveSignature(value.type().name()) + ")L" + wrapperTypeName.replace('.', '/') + ";";


        Method method = DebuggerUtils.findMethod(wrapperClass, "valueOf", methodSignature);
        if (method == null) {
            method = DebuggerUtils.findMethod(wrapperClass, "<init>", methodSignature);
        }

        if (method == null) {
            throw new EvaluateException("Cannot construct wrapper object for value of type " + value

                    .type() + ": Unable to find either valueOf() or constructor method");
        }


        Method finalMethod = method;
        List<PrimitiveValue> args = Collections.singletonList(value);
        ThreadReference thread = context.getStackFrameProxy().threadProxy().getThreadReference();
        try {
            return wrapperClass.invokeMethod(thread, finalMethod, args, 1);
        } catch (Exception e) {
            throw new EvaluateException(e.getMessage(), e);
        }
    }

    public Object evaluate(EvaluationContext context) throws EvaluateException {
        return box(this.myOperand.evaluate(context), context);
    }
}

