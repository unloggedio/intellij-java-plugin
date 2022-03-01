package com.insidious.plugin.extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.engine.evaluation.expression.Modifier;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.Value;
import com.insidious.plugin.extension.DebuggerBundle;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import com.insidious.plugin.extension.evaluation.EvaluatorUtil;
import org.jetbrains.annotations.NotNull;


public class AssignmentEvaluator
        implements Evaluator {
    private final Evaluator myLeftEvaluator;
    private final Evaluator myRightEvaluator;

    public AssignmentEvaluator(@NotNull Evaluator leftEvaluator, @NotNull Evaluator rightEvaluator) {
        this.myLeftEvaluator = leftEvaluator;
        this.myRightEvaluator = DisableGC.create(rightEvaluator);
    }

    static void assign(Modifier modifier, Object right, EvaluationContext context) throws EvaluateException {
        if (modifier == null) {
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.not.lvalue"));
        }
        try {
            modifier.setValue((Value) right);
        } catch (ClassNotLoadedException e) {
            if (!context.isAutoLoadClasses()) {
                throw EvaluateExceptionUtil.createEvaluateException(e);
            }
            try {
                EvaluatorUtil.loadClass(context, e
                        .className(), context.getStackFrameProxy().getClassLoader());
            } catch (InvocationException | InvalidTypeException | com.sun.jdi.IncompatibleThreadStateException | ClassNotLoadedException e1) {


                throw EvaluateExceptionUtil.createEvaluateException(e1);
            }
        } catch (InvalidTypeException e) {
            throw EvaluateExceptionUtil.createEvaluateException(e);
        }
    }

    public Object evaluate(EvaluationContext context) throws EvaluateException {
        this.myLeftEvaluator.evaluate(context);
        Modifier modifier = this.myLeftEvaluator.getModifier();

        Object right = this.myRightEvaluator.evaluate(context);
        if (right != null && !(right instanceof Value)) {
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.not.rvalue"));
        }

        assign(modifier, right, context);

        return right;
    }

    public Modifier getModifier() {
        return this.myLeftEvaluator.getModifier();
    }


    public String toString() {
        return this.myLeftEvaluator + " = " + this.myRightEvaluator;
    }
}

