package com.insidious.plugin.ui.Highlighter;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.ui.UIUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
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
        if (element instanceof PsiIdentifier &&
                element.getParent() instanceof PsiMethod) {

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
        InsidiousService.GUTTER_STATE state = project.getService(InsidiousService.class).getGutterStateFor(method);
        logger.warn("Get unlogged gutter icon for: " + method.getName() + " at state [" + state + "]");
        switch (state) {
            case NO_DIFF:
                return UIUtils.NO_DIFF_GUTTER;
            case DIFF:
                return UIUtils.DIFF_GUTTER;
            case NO_AGENT:
                return UIUtils.NO_AGENT_GUTTER;
            default:
                return UIUtils.RE_EXECUTE;
        }
    }
}