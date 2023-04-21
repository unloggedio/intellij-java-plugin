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

    private final Pattern testFileNamePattern = Pattern.compile("^Test.*V.java$");
    private final Pattern testMethodNamePattern = Pattern.compile("^test.*");
    private final Supplier<String> accessibleNameProvider = () -> "Execute method";
    private final Map<GutterState, UnloggedGutterNavigationHandler> navHandlerMap = new HashMap<>();

    public LineHighlighter() {
        for (GutterState value : GutterState.values()) {
            navHandlerMap.put(value, new UnloggedGutterNavigationHandler(value));
        }
    }

    public LineMarkerInfo<PsiIdentifier> getLineMarkerInfo(@NotNull PsiElement element) {

        if (element instanceof PsiIdentifier && element.getParent() instanceof PsiMethod) {
//            InsidiousService insidiousService = element.getProject().getService(InsidiousService.class);

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
            GutterState gutterStateForMethod = getGutterStateForMethod(psiMethod);
//            System.out.println("[GOT STATE] {"+gutterStateForMethod.toString()+"} FOR METHOD {"+psiMethod.getName()+"}");
            Icon gutterIcon = UIUtils.getGutterIconForState(gutterStateForMethod);

            LineMarkerInfo<PsiIdentifier> psiIdentifierLineMarkerInfo = new LineMarkerInfo<>((PsiIdentifier) element,
                    element.getTextRange(), gutterIcon, psiIdentifier -> gutterStateForMethod.getToolTipText(),
                    navHandlerMap.get(gutterStateForMethod), GutterIconRenderer.Alignment.LEFT,
                    gutterStateForMethod.getAccessibleTextProvider());

//            switch (state) {
//
//                case NO_AGENT:
//                    break;
//                case EXECUTE:
//                    break;
//                case DIFF:
//                    break;
//                case NO_DIFF:
//                    break;
//                case PROCESS_NOT_RUNNING:
//                    break;
//                case PROCESS_RUNNING:
//                    break;
//                case DATA_AVAILABLE:
//                    new GotItTooltip("io.unlogged.gutter." + DATA_AVAILABLE, "New candidates processed",
//                            insidiousService)
//                            .show(psiIdentifierLineMarkerInfo, GotItTooltip.TOP_MIDDLE);
//                    break;
//            }

            return psiIdentifierLineMarkerInfo;
        }
        return null;
    }

    public GutterState getGutterStateForMethod(PsiMethod method) {
        return method.getProject().getService(InsidiousService.class)
                .getGutterStateFor(new JavaMethodAdapter(method));
    }
}