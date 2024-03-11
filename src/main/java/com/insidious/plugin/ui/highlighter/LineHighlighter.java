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
import com.intellij.psi.*;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LineHighlighter implements LineMarkerProvider {

    private static final Logger logger = LoggerUtil.getInstance(LineHighlighter.class);
    private static final Pattern testFileNamePattern = Pattern.compile("^Test.*V.java$");
    private static final Pattern testPreviewFilePattern = Pattern.compile("^.*_Unlogged_Preview.java$");
    private static final Pattern testPathFilePattern = Pattern.compile(".*src/test/java/.*");
    private static final Pattern testMethodNamePattern = Pattern.compile("^test.*");
    private static final Supplier<String> accessibleNameProvider = () -> "Execute method";
    private final Map<GutterState, UnloggedGutterNavigationHandler> navHandlerMap = new HashMap<>();
    private final Map<Integer, Boolean> mockCallIdentifierOnLineNumber = new HashMap<>();

    public LineHighlighter() {
        for (GutterState value : GutterState.values()) {
            navHandlerMap.put(value, new UnloggedGutterNavigationHandler(value));
        }
    }

    public LineMarkerInfo<PsiIdentifier> getLineMarkerInfo(PsiElement element) {

        if (element instanceof PsiIdentifier && element.getParent() instanceof PsiMethod) {

            if (element.getContainingFile() == null) {
                return null;
            }

            Matcher fileMatcher = testFileNamePattern.matcher(element.getContainingFile().getName());
            if (fileMatcher.matches()) {
                return null;
            }
            Matcher previewFileMatcher = testPreviewFilePattern.matcher(element.getContainingFile().getName());
            if (previewFileMatcher.matches()) {
                return null;
            }

            if (element.getContainingFile().getContainingDirectory() == null) {
                return null;
            }

            Matcher pathFileMatcher =
                    testPathFilePattern.matcher(element.getContainingFile().getContainingDirectory().toString());
            if (pathFileMatcher.matches()) {
                return null;
            }

            PsiMethod psiMethod = (PsiMethod) element.getParent();
            if (psiMethod.isConstructor()) {
                return null;
            }
            if (psiMethod.getName().equals("main")
                    && psiMethod.getModifierList().hasModifierProperty(PsiModifier.STATIC)
            ) {
                return null;
            }
            if (psiMethod.getContainingClass() instanceof PsiAnonymousClass) {
                return null;
            }
            GutterState gutterStateForMethod = GutterState.PROCESS_RUNNING;
            final Icon gutterIcon = UIUtils.EXECUTE;

            return new LineMarkerInfo<>(
                    (PsiIdentifier) element,
                    element.getTextRange(), gutterIcon, psiIdentifier -> gutterStateForMethod.getToolTipText(),
                    navHandlerMap.get(gutterStateForMethod), GutterIconRenderer.Alignment.LEFT);
        }
        return null;
    }
}