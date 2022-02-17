package extension;

import com.intellij.psi.PsiClass;
import com.intellij.util.Range;
import extension.evaluation.JVMName;
import extension.evaluation.JVMNameUtil;

public class ConstructorStepMethodFilter
        extends BasicStepMethodFilter {
    public ConstructorStepMethodFilter(JVMName classJvmName, Range<Integer> callingExpressionLines) {
        super(classJvmName, "<init>", null, 0, callingExpressionLines, false);
    }

    public ConstructorStepMethodFilter(PsiClass psiClass, Range<Integer> callingExpressionLines) {
        this(JVMNameUtil.getJVMQualifiedName(psiClass), callingExpressionLines);
    }
}


