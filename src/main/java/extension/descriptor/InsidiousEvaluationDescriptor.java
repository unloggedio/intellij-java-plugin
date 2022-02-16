package extension.descriptor;

import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.engine.evaluation.TextWithImports;
import com.intellij.debugger.engine.evaluation.expression.Modifier;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiCodeFragment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionCodeFragment;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.frame.XValueModifier;
import com.sun.jdi.*;
import extension.DebuggerBundle;
import extension.connector.InsidiousStackFrameProxy;
import extension.evaluation.EvaluationContext;
import extension.evaluation.EvaluatorUtil;
import extension.evaluation.expression.EvaluatorBuilder;
import extension.evaluation.expression.EvaluatorBuilderImpl;
import extension.evaluation.expression.ExpressionEvaluator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

public abstract class InsidiousEvaluationDescriptor extends InsidiousValueDescriptorImpl {
    protected TextWithImports myText;
    private Modifier myModifier;

    protected InsidiousEvaluationDescriptor(TextWithImports text, Project project, Value value) {
        super(project, value);
        this.myText = text;
    }

    protected InsidiousEvaluationDescriptor(TextWithImports text, Project project) {
        super(project);
        setLvalue(false);
        this.myText = text;
    }


    public String getName() {
        return this.myText.getText();
    }


    public PsiCodeFragment createCodeFragment(PsiElement context) {
        TextWithImports text = getEvaluationText();
        return DebuggerUtilsEx.findAppropriateCodeFragmentFactory(text, context)
                .createCodeFragment(text, context, this.myProject);
    }


    public final Value calcValue(EvaluationContext evaluationContext) throws EvaluateException {
        try {
//            ApplicationManager.getApplication()
//                    .invokeLater(() -> PsiDocumentManager.getInstance(this.myProject).commitAndRunReadAction(()));


            EvaluationContext thisEvaluationContext = getEvaluationContext(evaluationContext);
            SourcePosition position = EvaluatorUtil.getTopFrameSourcePosition(evaluationContext);

            AtomicReference<PsiCodeFragment> code = new AtomicReference<>();

            ExpressionEvaluator evaluator = ReadAction.compute(() -> {
                code.set(getEvaluationCode(thisEvaluationContext));


                EvaluatorBuilder evaluatorBuilder = EvaluatorBuilderImpl.getInstance();

                return evaluatorBuilder.build(code.get(), position, evaluationContext);
            });

            if (!thisEvaluationContext.getVirtualMachineProxy().isAttached()) {
                throw EvaluateExceptionUtil.PROCESS_EXITED;
            }
            InsidiousStackFrameProxy InsidiousStackFrameProxy = thisEvaluationContext.getStackFrameProxy();
            if (InsidiousStackFrameProxy == null) {
                throw EvaluateExceptionUtil.NULL_STACK_FRAME;
            }

            Value value = evaluator.evaluate(thisEvaluationContext);

            this.myModifier = evaluator.getModifier();
            setLvalue((this.myModifier != null));

            return value;
        } catch (IndexNotReadyException ex) {
            throw new EvaluateException("Evaluation is not possible during indexing", ex);
        } catch (EvaluateException ex) {
            throw new EvaluateException(ex.getLocalizedMessage(), ex);
        } catch (ObjectCollectedException ex) {
            throw EvaluateExceptionUtil.OBJECT_WAS_COLLECTED;
        }
    }


    public PsiExpression getDescriptorEvaluation(EvaluationContext context) throws EvaluateException {
        PsiCodeFragment psiCodeFragment = getEvaluationCode(context);
        if (psiCodeFragment instanceof PsiExpressionCodeFragment) {
            return ((PsiExpressionCodeFragment) psiCodeFragment).getExpression();
        }
        throw new EvaluateException(
                DebuggerBundle.message("error.cannot.create.expression.from.code.fragment"), null);
    }


    protected boolean isPrintExceptionToConsole() {
        return false;
    }

    @Nullable
    public Modifier getModifier() {
        return this.myModifier;
    }


    public boolean canSetValue() {
        return (super.canSetValue() && this.myModifier != null && this.myModifier.canSetValue());
    }

    public TextWithImports getEvaluationText() {
        return this.myText;
    }


    public XValueModifier getModifier(InsidiousJavaValue value) {
        return new InsidiousJavaValueModifier(value) {
            protected void setValueImpl(@NotNull XExpression expression, @NotNull XValueModifier.XModificationCallback callback) {
                final InsidiousEvaluationDescriptor evaluationDescriptor = InsidiousEvaluationDescriptor.this;
                if (evaluationDescriptor.canSetValue())
                    set(expression, callback, InsidiousEvaluationDescriptor.this


                            .getProject(), new InsidiousJavaValueModifier.SetValueRunnable() {


                        public void setValue(EvaluationContext evaluationContext, Value newValue) throws ClassNotLoadedException, InvalidTypeException, EvaluateException, IncompatibleThreadStateException {
                            evaluationDescriptor.getModifier().setValue(newValue);
                            InsidiousJavaValueModifier.update(evaluationContext);
                        }


                        @NotNull
                        public Type getLType() throws EvaluateException, ClassNotLoadedException {
                            return evaluationDescriptor.getModifier().getExpectedType();
                        }
                    });
            }
        };
    }

    protected abstract EvaluationContext getEvaluationContext(EvaluationContext paramEvaluationContext);

    protected abstract PsiCodeFragment getEvaluationCode(EvaluationContext paramEvaluationContext) throws EvaluateException;
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\descriptor\InsidiousEvaluationDescriptor.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */