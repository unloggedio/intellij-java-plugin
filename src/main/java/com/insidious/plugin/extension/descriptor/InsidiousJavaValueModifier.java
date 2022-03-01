package com.insidious.plugin.extension.descriptor;

import com.intellij.debugger.DebuggerInvocationUtil;
import com.intellij.debugger.EvaluatingComputable;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.engine.evaluation.TextWithImportsImpl;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XValueModifier;
import com.sun.jdi.*;
import com.insidious.plugin.extension.DebuggerBundle;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import com.insidious.plugin.extension.evaluation.EvaluatorUtil;
import com.insidious.plugin.extension.evaluation.expression.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class InsidiousJavaValueModifier extends XValueModifier {
    private final InsidiousJavaValue myJavaValue;

    public InsidiousJavaValueModifier(InsidiousJavaValue javaValue) {
        this.myJavaValue = javaValue;
    }

    protected static void update(EvaluationContext context) {
        context.getVirtualMachineProxy().getXDebugProcess().getSession().rebuildViews();
    }

    protected static Value preprocessValue(EvaluationContext context, Value value, @NotNull Type varType) throws EvaluateException {
        if (value != null && "java.lang.String"
                .equals(varType.name()) && !(value instanceof com.sun.jdi.StringReference)) {

            String v = EvaluatorUtil.getValueAsString(context, value);
            if (v != null) {
                value = context.getVirtualMachineProxy().getVirtualMachine().mirrorOf(v);
            }
        }
        if (value instanceof DoubleValue) {
            double dValue = ((DoubleValue) value).doubleValue();
            if (varType instanceof com.sun.jdi.FloatType && 1.401298464324817E-45D <= dValue && dValue <= 3.4028234663852886E38D) {


                value = context.getVirtualMachineProxy().getVirtualMachine().mirrorOf((float) dValue);
            }
        }
        if (value != null) {
            if (varType instanceof com.sun.jdi.PrimitiveType) {
                if (!(value instanceof com.sun.jdi.PrimitiveValue)) {
                    value = (Value) UnBoxingEvaluator.unbox(value, context);
                }
            } else if (varType instanceof ReferenceType &&
                    value instanceof com.sun.jdi.PrimitiveValue) {
                value = (Value) BoxingEvaluator.box(value, context);
            }
        }

        return value;
    }

    @Nullable
    private static ExpressionEvaluator tryDirectAssignment(@NotNull XExpression expression, @Nullable Type varType, @NotNull EvaluationContext evaluationContext) {
        if (varType instanceof com.sun.jdi.LongType) {
            try {
                return new ExpressionEvaluatorImpl(new IdentityEvaluator(evaluationContext


                        .getVirtualMachineProxy()
                        .getVirtualMachine()
                        .mirrorOf(Long.decode(expression.getExpression()).longValue())));
            } catch (NumberFormatException numberFormatException) {
            }
        }

        return null;
    }

    private static void setValue(ExpressionEvaluator evaluator, EvaluationContext evaluationContext, SetValueRunnable setValueRunnable) throws EvaluateException {
        try {
            Value value = evaluator.evaluate(evaluationContext);

            setValueRunnable.setValue(evaluationContext, value);
        } catch (IllegalArgumentException ex) {
            throw EvaluateExceptionUtil.createEvaluateException(ex.getMessage());
        } catch (InvalidTypeException ex) {
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.type.mismatch"));
        } catch (IncompatibleThreadStateException e) {
            throw EvaluateExceptionUtil.createEvaluateException(e);
        } catch (ClassNotLoadedException ex) {
            if (!evaluationContext.isAutoLoadClasses()) {
                throw EvaluateExceptionUtil.createEvaluateException(ex);
            }


            try {
                ReferenceType refType = EvaluatorUtil.loadClass(evaluationContext, ex

                        .className(), setValueRunnable
                        .getClassLoader(evaluationContext));
                if (refType != null) {
                    setValue(evaluator, evaluationContext, setValueRunnable);
                }
            } catch (InvocationException | InvalidTypeException | IncompatibleThreadStateException | ClassNotLoadedException e) {


                throw EvaluateExceptionUtil.createEvaluateException(e);
            } catch (ObjectCollectedException e) {
                throw EvaluateExceptionUtil.OBJECT_WAS_COLLECTED;
            }
        }
    }

    public void calculateInitialValueEditorText(XInitialValueCallback callback) {
        Value value = this.myJavaValue.getDescriptor().getValue();
        if (value == null || value instanceof com.sun.jdi.PrimitiveValue) {
            String valueString = this.myJavaValue.getDescriptor().getValueText();
            int pos = valueString.lastIndexOf('(');
            if (pos > 1) {
                valueString = valueString.substring(0, pos).trim();
            }
            callback.setValue(valueString);
        } else if (value instanceof com.sun.jdi.StringReference) {
            EvaluationContext evaluationContext = this.myJavaValue.getEvaluationContext();
            try {
                callback.setValue(
                        StringUtil.wrapWithDoubleQuote(
                                DebuggerUtils.translateStringValue(
                                        EvaluatorUtil.getValueAsString(evaluationContext, value))));
            } catch (EvaluateException evaluateException) {
            }
        } else {

            callback.setValue(null);
        }
    }

    protected abstract void setValueImpl(@NotNull XExpression paramXExpression, @NotNull XValueModifier.XModificationCallback paramXModificationCallback);

    public void setValue(@NotNull XExpression expression, @NotNull XValueModifier.XModificationCallback callback) {
        InsidiousValueDescriptorImpl descriptor = this.myJavaValue.getDescriptor();
        if (!descriptor.canSetValue()) {
            return;
        }

        if (this.myJavaValue.getEvaluationContext().getXSuspendContext() == null) {
            callback.errorOccurred(DebuggerBundle.message("error.context.has.changed"));

            return;
        }
        setValueImpl(expression, callback);
    }

    protected void set(@NotNull final XExpression expression, XModificationCallback callback, final Project project, final SetValueRunnable setValueRunnable) {
        final ProgressWindow progressWindow = new ProgressWindow(true, project);
        final EvaluationContext evaluationContext = this.myJavaValue.getEvaluationContext();


        try {
            ExpressionEvaluator evaluator = tryDirectAssignment(expression, setValueRunnable.getLType(), evaluationContext);

            if (evaluator == null) {


                XSourcePosition xPosition = evaluationContext.getXSuspendContext().getActiveExecutionStack().getTopFrame().getSourcePosition();
                final SourcePosition position = DebuggerUtilsEx.toSourcePosition(xPosition, project);
                final PsiElement context = position.getElementAt();

                evaluator = DebuggerInvocationUtil.commitAndRunReadAction(project, new EvaluatingComputable<ExpressionEvaluator>() {

                    public ExpressionEvaluator compute() throws EvaluateException {
                        return EvaluatorBuilderImpl.build(
                                TextWithImportsImpl.fromXExpression(expression), context, position, project, evaluationContext);
                    }
                });
            }


            setValue(evaluator, evaluationContext, new SetValueRunnable() {


                public void setValue(EvaluationContext evaluationContext, Value newValue) throws ClassNotLoadedException, InvalidTypeException, EvaluateException, IncompatibleThreadStateException {
                    if (!progressWindow.isCanceled()) {
                        setValueRunnable.setValue(evaluationContext, newValue);
                    }
                }


                @Nullable
                public Type getLType() throws EvaluateException, ClassNotLoadedException {
                    return setValueRunnable.getLType();
                }
            });
            callback.valueModified();
        } catch (EvaluateException | ClassNotLoadedException e) {
            callback.errorOccurred(e.getMessage());
        }
    }


    protected interface SetValueRunnable {
        void setValue(EvaluationContext param1EvaluationContext, Value param1Value) throws ClassNotLoadedException, InvalidTypeException, EvaluateException, IncompatibleThreadStateException;


        default ClassLoaderReference getClassLoader(EvaluationContext evaluationContext) throws EvaluateException {
            return evaluationContext.getStackFrameProxy().getClassLoader();
        }


        @Nullable
        Type getLType() throws ClassNotLoadedException, EvaluateException;
    }
}

