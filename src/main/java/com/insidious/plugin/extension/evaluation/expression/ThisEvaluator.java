package com.insidious.plugin.extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;
import com.insidious.plugin.extension.DebuggerBundle;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public class ThisEvaluator
        implements Evaluator {
    private final int myIterations;

    public ThisEvaluator() {
        this.myIterations = 0;
    }

    public ThisEvaluator(int iterations) {
        this.myIterations = iterations;
    }

    @Nullable
    private static ObjectReference getOuterObject(ObjectReference objRef) {
        if (objRef == null) {
            return null;
        }
        List<Field> list = objRef.referenceType().fields();
        for (Field field : list) {
            String name = field.name();
            if (name != null && name.startsWith("this$")) {
                ObjectReference rv = (ObjectReference) objRef.getValue(field);
                if (rv != null) {
                    return rv;
                }
            }
        }
        return null;
    }

    public Object evaluate(EvaluationContext context) throws EvaluateException {
        Value objRef = context.computeThisObject();
        if (this.myIterations > 0) {
            ObjectReference thisRef = (ObjectReference) objRef;
            for (int idx = 0; idx < this.myIterations && thisRef != null; idx++) {
                thisRef = getOuterObject(thisRef);
            }
            objRef = thisRef;
        }
        if (objRef == null) {
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.this.not.avalilable", new Object[0]));
        }
        return objRef;
    }

    public String toString() {
        return "this";
    }
}


