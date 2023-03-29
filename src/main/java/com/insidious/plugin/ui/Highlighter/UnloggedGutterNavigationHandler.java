package com.insidious.plugin.ui.Highlighter;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;

import java.awt.event.MouseEvent;

public class UnloggedGutterNavigationHandler implements GutterIconNavigationHandler {

    @Override
    public void navigate(MouseEvent e, PsiElement element) {
        if (element instanceof PsiIdentifier && element.getParent() instanceof PsiMethod) {
            PsiMethod method = (PsiMethod) element.getParent();
            PsiClass psiClass = PsiTreeUtil.findElementOfClassAtOffset(element.getContainingFile(),
                    element.getTextOffset(), PsiClass.class, false);
            InsidiousService insidiousService = psiClass.getProject().getService(InsidiousService.class);
            insidiousService.openTestCaseDesigner(psiClass.getProject());
            insidiousService.methodFocussedHandler(psiClass, method, null);
            UsageInsightTracker.getInstance().RecordEvent(
                    "TestIconClick",null);
        }
    }
}
