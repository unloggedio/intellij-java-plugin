package extension.descriptor;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpression;
import com.intellij.util.IncorrectOperationException;
import com.sun.jdi.Value;
import extension.evaluation.EvaluationContext;

public class InsidiousThisDescriptorImpl extends InsidiousValueDescriptorImpl {
    public InsidiousThisDescriptorImpl(Project project) {
        super(project);
    }


    public Value calcValue(EvaluationContext evaluationContext) throws EvaluateException {
        return (evaluationContext != null) ? evaluationContext.computeThisObject() : null;
    }


    public String getName() {
        return "this";
    }


    public PsiExpression getDescriptorEvaluation(EvaluationContext context) throws EvaluateException {
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(this.myProject);
        try {
            return elementFactory.createExpressionFromText("this", null);
        } catch (IncorrectOperationException e) {
            throw new EvaluateException(e.getMessage(), e);
        }
    }


    public boolean canSetValue() {
        return false;
    }
}

