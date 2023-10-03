package com.insidious.plugin.ui.highlighter;

import com.insidious.plugin.adapter.kotlin.KotlinMethodAdapter;
import com.insidious.plugin.factory.GutterState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.util.UIUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import com.sun.istack.NotNull;
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken;
import org.jetbrains.kotlin.psi.KtNamedFunction;

import javax.swing.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KotlinLineHighlighter implements LineMarkerProvider {

    private static final Logger logger = LoggerUtil.getInstance(KotlinLineHighlighter.class);
    private final KotlinUnloggedGutterNavigationHandler navHandler = new KotlinUnloggedGutterNavigationHandler();
    private final Pattern testFileNamePattern = Pattern.compile("^Test.*V.kt$");
    private final Pattern testMethodNamePattern = Pattern.compile("^test.*");
    private final Supplier<String> accessibleNameProvider = () -> "Execute method";

    public LineMarkerInfo<LeafPsiElement> getLineMarkerInfo( PsiElement element) {
        if (element instanceof LeafPsiElement &&
                element.getParent() instanceof KtNamedFunction) {
            IElementType elementType = ((LeafPsiElement) element).getElementType();
//            if (elementType instanceof WhiteSpaceTokenType) {
//                return null;
//            }
            if (elementType instanceof KtModifierKeywordToken) {
                return null;
            }
            String elementTypeString = elementType.toString();
            if (elementTypeString.equals("COLON") || elementTypeString.equals("WHITE_SPACE")) {
                return null;
            }
            Matcher fileMatcher = testFileNamePattern.matcher(element.getContainingFile().getName());
            if (fileMatcher.matches()) {
                return null;
            }
            KtNamedFunction psiMethod = (KtNamedFunction) element.getParent();
            Icon gutter_Icon = getIconForState(psiMethod);
            return new LineMarkerInfo<LeafPsiElement>((LeafPsiElement) element,
                    element.getTextRange(), gutter_Icon, null, navHandler,
                    GutterIconRenderer.Alignment.LEFT);
        }
        return null;
    }

    public Icon getIconForState(KtNamedFunction method) {
        Project project = method.getProject();
        GutterState state = project
                .getService(InsidiousService.class)
                .getGutterStateFor(new KotlinMethodAdapter(method));
//        logger.warn("Get unlogged gutter icon for: " + method.getName() + " at state [" + state + "]");
        switch (state) {
            case NO_DIFF:
                return UIUtils.NO_DIFF_GUTTER;
            case DIFF:
                return UIUtils.DIFF_GUTTER;
//            case NO_AGENT:
//                return UIUtils.NO_AGENT_GUTTER;
            default:
                return UIUtils.RE_EXECUTE;
        }
    }
}