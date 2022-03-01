package com.insidious.plugin.extension.descriptor;

import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.ui.tree.NodeDescriptor;
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
import com.insidious.plugin.extension.thread.InsidiousLocalVariableProxy;
import com.insidious.plugin.extension.connector.InsidiousStackFrameProxy;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import com.insidious.plugin.extension.evaluation.EvaluatorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InsidiousLocalVariableDescriptorImpl extends InsidiousValueDescriptorImpl implements InsidiousLocalVariableDescriptor {
    private final InsidiousStackFrameProxy myFrameProxy;
    private final InsidiousLocalVariableProxy myLocalVariable;
    private String myTypeName = DebuggerBundle.message("label.unknown.value");

    private boolean myIsPrimitive;
    private boolean myIsParameter;
    private boolean myIsNewLocal = true;

    public InsidiousLocalVariableDescriptorImpl(Project project, @NotNull InsidiousLocalVariableProxy local) {
        super(project);
        setLvalue(true);
        this.myFrameProxy = local.getFrame();
        this.myLocalVariable = local;
    }


    public InsidiousLocalVariableProxy getLocalVariable() {
        return this.myLocalVariable;
    }

    public boolean isNewLocal() {
        return this.myIsNewLocal;
    }

    public void setNewLocal(boolean aNew) {
        this.myIsNewLocal = aNew;
    }

    public boolean isPrimitive() {
        return this.myIsPrimitive;
    }

    public Value calcValue(EvaluationContext evaluationContext) throws EvaluateException {
        InsidiousLocalVariableProxy variable = getLocalVariable();
        boolean isVisible = variable.isVisible(this.myFrameProxy);
        if (isVisible) {
            String typeName = variable.typeName();
            this.myTypeName = typeName;
            this.myIsPrimitive = DebuggerUtils.isPrimitiveType(typeName);
            this.myIsParameter = variable.isArgument();
            return this.myFrameProxy.getValue(variable);
        }

        return null;
    }

    public void displayAs(NodeDescriptor descriptor) {
        super.displayAs(descriptor);
        if (descriptor instanceof InsidiousLocalVariableDescriptorImpl) {
            this.myIsNewLocal = ((InsidiousLocalVariableDescriptorImpl) descriptor).myIsNewLocal;
        }
    }


    public String getName() {
        return this.myLocalVariable.name();
    }


    @Nullable
    public String getDeclaredType() {
        return this.myTypeName;
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
                final InsidiousLocalVariableProxy local = InsidiousLocalVariableDescriptorImpl.this.getLocalVariable();
                if (local != null) {
                    set(expression, callback, InsidiousLocalVariableDescriptorImpl.this


                            .getProject(), new InsidiousJavaValueModifier.SetValueRunnable() {


                        public void setValue(EvaluationContext evaluationContext, Value newValue) throws ClassNotLoadedException, InvalidTypeException, EvaluateException {
                            evaluationContext
                                    .getStackFrameProxy()
                                    .setValue(local,

                                            InsidiousJavaValueModifier.preprocessValue(evaluationContext, newValue,


                                                    getLType()));
                            InsidiousJavaValueModifier.update(evaluationContext);
                        }


                        @NotNull
                        public Type getLType() throws EvaluateException, ClassNotLoadedException {
                            return local.getType();
                        }
                    });
                }
            }
        };
    }

    public boolean isParameter() {
        return this.myIsParameter;
    }
}

