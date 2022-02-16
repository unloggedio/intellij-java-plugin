package extension.evaluation.expression;

import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.psi.PsiElement;
import extension.evaluation.EvaluationContext;

public interface EvaluatorBuilder {
    ExpressionEvaluator build(PsiElement paramPsiElement, SourcePosition paramSourcePosition, EvaluationContext paramEvaluationContext) throws EvaluateException;
}

