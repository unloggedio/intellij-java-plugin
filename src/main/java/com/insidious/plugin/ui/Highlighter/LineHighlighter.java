package com.insidious.plugin.ui.Highlighter;

import com.insidious.plugin.ui.UIUtils;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.*;
import com.sun.istack.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LineHighlighter implements LineMarkerProvider {
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
        if (element instanceof PsiIdentifier &&
                element.getParent() instanceof PsiMethod) {
            Pattern file = Pattern.compile("^Test.*V.java$");
            Matcher fileMatcher = file.matcher(element.getContainingFile().getName());
            Pattern method = Pattern.compile("^test.*");
            PsiMethod psiMethod = (PsiMethod) element.getParent();
            PsiModifierList modifierList = psiMethod.getModifierList();
            if (modifierList.hasModifierProperty(PsiModifier.PRIVATE) ||
                    modifierList.hasModifierProperty(PsiModifier.PROTECTED)) {
                return null;
            }
            Matcher methodMatcher = method.matcher(element.getText());
            if(!(fileMatcher.matches() && methodMatcher.matches()))
            {
                LineMarkerInfo info = new LineMarkerInfo(element,
                        element.getTextRange(),
                        UIUtils.TEST_TUBE_FILL,
                        null,
                        new UnloggedGutterNavigationHandler(),
                        GutterIconRenderer.Alignment.LEFT);
                return info;
            }
        }
        return null;
    }
}