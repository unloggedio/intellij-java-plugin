package com.insidious.plugin.extension.smartstep;

import com.intellij.debugger.SourcePosition;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiStatement;
import com.intellij.util.Range;
import org.jetbrains.annotations.Nullable;

public class AnonymousClassMethodFilter extends BasicStepMethodFilter implements BreakpointStepMethodFilter {
    @Nullable
    private final SourcePosition myBreakpointPosition;
    private final int myLastStatementLine;

    public AnonymousClassMethodFilter(PsiMethod psiMethod, Range<Integer> lines) {
        super(psiMethod, lines);
        SourcePosition firstStatementPosition = null;
        SourcePosition lastStatementPosition = null;
        PsiElement navigationElement = psiMethod.getNavigationElement();
        if (navigationElement instanceof PsiMethod) {
            psiMethod = (PsiMethod) navigationElement;
        }
        PsiCodeBlock body = psiMethod.getBody();
        if (body != null) {
            PsiStatement[] statements = body.getStatements();
            if (statements.length > 0) {
                firstStatementPosition = SourcePosition.createFromElement(statements[0]);
                if (firstStatementPosition != null) {
                    PsiStatement lastStatement = statements[statements.length - 1];

                    lastStatementPosition = SourcePosition.createFromOffset(firstStatementPosition
                            .getFile(), lastStatement
                            .getTextRange().getEndOffset());
                }
            }
        }
        this.myBreakpointPosition = firstStatementPosition;
        this.myLastStatementLine = (lastStatementPosition != null) ? lastStatementPosition.getLine() : -1;
    }


    @Nullable
    public SourcePosition getBreakpointPosition() {
        return this.myBreakpointPosition;
    }


    public int getLastStatementLine() {
        return this.myLastStatementLine;
    }
}


