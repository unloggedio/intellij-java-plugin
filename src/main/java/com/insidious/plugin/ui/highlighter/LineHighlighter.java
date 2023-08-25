package com.insidious.plugin.ui.highlighter;

import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.factory.GutterState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.sun.istack.NotNull;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LineHighlighter implements LineMarkerProvider {

    private static final Logger logger = LoggerUtil.getInstance(LineHighlighter.class);
    private static final Pattern testFileNamePattern = Pattern.compile("^Test.*V.java$");
    private static final Pattern testMethodNamePattern = Pattern.compile("^test.*");
    private static final Supplier<String> accessibleNameProvider = () -> "Execute method";
    private final Map<GutterState, UnloggedGutterNavigationHandler> navHandlerMap = new HashMap<>();

    public LineHighlighter() {
        for (GutterState value : GutterState.values()) {
            navHandlerMap.put(value, new UnloggedGutterNavigationHandler(value));
        }
    }

    public LineMarkerInfo<PsiIdentifier> getLineMarkerInfo(@NotNull PsiElement element) {

        if (element instanceof PsiIdentifier && element.getParent() instanceof PsiMethod) {

            if (element.getContainingFile() == null) {
                return null;
            }

            Matcher fileMatcher = testFileNamePattern.matcher(element.getContainingFile().getName());
            if (fileMatcher.matches()) {
                return null;
            }


            PsiMethod psiMethod = (PsiMethod) element.getParent();
            if (psiMethod.isConstructor()) {
                return null;
            }
            if (psiMethod.getContainingClass() instanceof PsiAnonymousClass) {
                return null;
            }
            GutterState gutterStateForMethod = getGutterStateForMethod(psiMethod);
            final Icon gutterIcon = UIUtils.getGutterIconForState(gutterStateForMethod);

            return new LineMarkerInfo<>(
                    (PsiIdentifier) element,
                    element.getTextRange(), gutterIcon, psiIdentifier -> gutterStateForMethod.getToolTipText(),
                    navHandlerMap.get(gutterStateForMethod), GutterIconRenderer.Alignment.LEFT);
        }
        return null;
    }

    public GutterState getGutterStateForMethod(PsiMethod method) {
        return method.getProject().getService(InsidiousService.class).getGutterStateFor(new JavaMethodAdapter(method));
    }
}