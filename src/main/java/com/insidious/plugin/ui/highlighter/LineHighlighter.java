package com.insidious.plugin.ui.highlighter;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.util.UIUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.sun.istack.NotNull;

import javax.swing.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LineHighlighter implements LineMarkerProvider {

    private static final Logger logger = LoggerUtil.getInstance(LineHighlighter.class);
    private final UnloggedGutterNavigationHandler navHandler = new UnloggedGutterNavigationHandler();
    private final Pattern testFileNamePattern = Pattern.compile("^Test.*V.java$");
    private final Pattern testMethodNamePattern = Pattern.compile("^test.*");
    private final Supplier<String> accessibleNameProvider = () -> "Execute method";

    public LineMarkerInfo<PsiIdentifier> getLineMarkerInfo(@NotNull PsiElement element) {
        if (element instanceof PsiIdentifier && element.getParent() instanceof PsiMethod) {

            Matcher fileMatcher = testFileNamePattern.matcher(element.getContainingFile().getName());
            if (fileMatcher.matches()) {
                return null;
            }
            PsiMethod psiMethod = (PsiMethod) element.getParent();
//            Matcher methodMatcher = testMethodNamePattern.matcher(element.getText());
//            if (methodMatcher.matches()) {
//                return null;
//            }
//            PsiModifierList modifierList = psiMethod.getModifierList();
            if (psiMethod.isConstructor()) {
                return null;
            }
//            if (modifierList.hasModifierProperty(PsiModifier.PRIVATE) ||
//                    modifierList.hasModifierProperty(PsiModifier.PROTECTED)) {
//                return null;
//            }
            Icon gutter_Icon = getIconForState(psiMethod);
            return new LineMarkerInfo<>((PsiIdentifier) element,
                    element.getTextRange(), gutter_Icon, null, navHandler,
                    GutterIconRenderer.Alignment.LEFT, accessibleNameProvider);
        }
        return null;
    }

    public Icon getIconForState(PsiMethod method) {
        Project project = method.getProject();
        InsidiousService.GUTTER_STATE state = project.getService(InsidiousService.class).getGutterStateFor(
                method);
        System.out.println("Got State : "+state+" for method "+method.getName());
//        logger.warn("Get unlogged gutter icon for: " + method.getName() + " at state [" + state + "]");
        return UIUtils.getGutterIconForState(state);
    }
}