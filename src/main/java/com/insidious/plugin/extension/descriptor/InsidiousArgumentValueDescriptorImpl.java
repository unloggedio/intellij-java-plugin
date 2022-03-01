package com.insidious.plugin.extension.descriptor;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpression;
import com.intellij.util.IncorrectOperationException;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.frame.XValueModifier;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.insidious.plugin.extension.DebuggerBundle;
import com.insidious.plugin.extension.connector.DecompiledLocalVariable;
import com.insidious.plugin.extension.connector.LocalVariablesUtil;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import com.insidious.plugin.extension.evaluation.EvaluatorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InsidiousArgumentValueDescriptorImpl extends InsidiousValueDescriptorImpl {
    private final DecompiledLocalVariable myVariable;

    public InsidiousArgumentValueDescriptorImpl(Project project, DecompiledLocalVariable variable, Value value) {
        super(project, value);
        this.myVariable = variable;
        setLvalue(true);
    }

    public boolean canSetValue() {
        return LocalVariablesUtil.canSetValues();
    }


    public boolean isPrimitive() {
        return getValue() instanceof com.sun.jdi.PrimitiveValue;
    }


    public Value calcValue(EvaluationContext evaluationContext) throws EvaluateException {
        return getValue();
    }

    public DecompiledLocalVariable getVariable() {
        return this.myVariable;
    }


    public String getName() {
        return this.myVariable.getDisplayName();
    }

    public boolean isParameter() {
        return this.myVariable.isParam();
    }


    public PsiExpression getDescriptorEvaluation(EvaluationContext context) throws EvaluateException {
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(this.myProject);
        try {
            return elementFactory.createExpressionFromText(
                    getName(), EvaluatorUtil.getContextElement(context));
        } catch (IncorrectOperationException e) {
            throw new EvaluateException(
                    DebuggerBundle.message("error.invalid.local.variable.name", getName()), e);
        }
    }


    public XValueModifier getModifier(InsidiousJavaValue value) {
        return new InsidiousJavaValueModifier(value) {
            protected void setValueImpl(@NotNull XExpression expression, @NotNull XValueModifier.XModificationCallback callback) {
                final DecompiledLocalVariable local = InsidiousArgumentValueDescriptorImpl.this.getVariable();
                if (local != null)
                    set(expression, callback, InsidiousArgumentValueDescriptorImpl.this


                            .getProject(), new InsidiousJavaValueModifier.SetValueRunnable() {


                        public void setValue(EvaluationContext evaluationContext, Value newValue) throws ClassNotLoadedException, InvalidTypeException, EvaluateException {
                            LocalVariablesUtil.setValue(
                                    evaluationContext.getStackFrameProxy().getStackFrame(), local.getSlot(), newValue);

                            InsidiousJavaValueModifier.update(evaluationContext);
                        }


                        @Nullable
                        public Type getLType() {
                            return null;
                        }
                    });
            }
        };
    }
}

