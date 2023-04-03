package com.insidious.plugin.ui.Highlighter;

import com.insidious.plugin.ui.UIUtils;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.*;
import com.sun.istack.NotNull;
import org.jetbrains.annotations.Nls;

import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LineHighlighter implements LineMarkerProvider {

    private final UnloggedGutterNavigationHandler navHandler = new UnloggedGutterNavigationHandler();
    private final Pattern testFileNamePattern = Pattern.compile("^Test.*V.java$");
    private final Pattern testMethodNamePattern = Pattern.compile("^test.*");
    private final @org.jetbrains.annotations.NotNull Supplier<@org.jetbrains.annotations.NotNull @Nls String> accessibleNameProvider = new Supplier<String>() {
        @Override
        public String get() {
            return "Execute method";
        }
    };

    public LineMarkerInfo<PsiIdentifier> getLineMarkerInfo(@NotNull PsiElement element) {
        if (element instanceof PsiIdentifier &&
                element.getParent() instanceof PsiMethod) {

            Matcher fileMatcher = testFileNamePattern.matcher(element.getContainingFile().getName());
            if (fileMatcher.matches()) {
                return null;
            }
            PsiMethod psiMethod = (PsiMethod) element.getParent();
            Matcher methodMatcher = testMethodNamePattern.matcher(element.getText());
            if (methodMatcher.matches()) {
                return null;
            }
            PsiModifierList modifierList = psiMethod.getModifierList();
            if (modifierList.hasModifierProperty(PsiModifier.PRIVATE) ||
                    modifierList.hasModifierProperty(PsiModifier.PROTECTED) ||
                    psiMethod.isConstructor()) {
                return null;
            }
            return new LineMarkerInfo<>((PsiIdentifier) element,
                    element.getTextRange(), UIUtils.TEST_BEAKER_TEAL, null, navHandler,
                    GutterIconRenderer.Alignment.LEFT, accessibleNameProvider);
        }
        return null;
    }
}