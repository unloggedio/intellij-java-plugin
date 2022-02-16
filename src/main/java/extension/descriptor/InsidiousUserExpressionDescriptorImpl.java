package extension.descriptor;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.engine.evaluation.TextWithImports;
import com.intellij.debugger.impl.DebuggerUtilsImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaCodeFragment;
import com.intellij.psi.PsiCodeFragment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.sun.jdi.Type;
import extension.DebuggerBundle;
import extension.evaluation.EvaluationContext;
import org.jetbrains.annotations.Nullable;


public class InsidiousUserExpressionDescriptorImpl
        extends InsidiousEvaluationDescriptor
        implements InsidiousUserExpressionDescriptor {
    private final InsidiousValueDescriptorImpl myParentDescriptor;
    private final String myTypeName;
    private final String myName;
    private final int myEnumerationIndex;

    public InsidiousUserExpressionDescriptorImpl(Project project, InsidiousValueDescriptorImpl parent, String typeName, String name, TextWithImports text, int enumerationIndex) {
        super(text, project);
        this.myParentDescriptor = parent;
        this.myTypeName = typeName;
        this.myName = name;
        this.myEnumerationIndex = enumerationIndex;
    }


    public String getName() {
        return StringUtil.isEmpty(this.myName) ? this.myText.getText() : this.myName;
    }


    @Nullable
    public String getDeclaredType() {
        Type type = getType();
        return (type != null) ? type.name() : null;
    }


    protected PsiCodeFragment getEvaluationCode(EvaluationContext context) throws EvaluateException {
        Pair<PsiElement, PsiType> psiClassAndType = DebuggerUtilsImpl.getPsiClassAndType(this.myTypeName, this.myProject);
        if (psiClassAndType.first == null) {
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.invalid.type.name", new Object[]{this.myTypeName}));
        }
        PsiCodeFragment fragment = createCodeFragment(psiClassAndType.first);
        if (fragment instanceof JavaCodeFragment) {
            ((JavaCodeFragment) fragment).setThisType(psiClassAndType.second);
        }
        return fragment;
    }

    public InsidiousValueDescriptorImpl getParentDescriptor() {
        return this.myParentDescriptor;
    }


    protected EvaluationContext getEvaluationContext(EvaluationContext evaluationContext) {
        return evaluationContext.createEvaluationContext(this.myParentDescriptor.getValue());
    }

    public int getEnumerationIndex() {
        return this.myEnumerationIndex;
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\descriptor\InsidiousUserExpressionDescriptorImpl.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */