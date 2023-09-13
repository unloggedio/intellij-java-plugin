package com.insidious.plugin.ui.highlighter;

import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Pattern;

public class MockMethodLineHighlighter implements LineMarkerProvider {

    private static final Logger logger = LoggerUtil.getInstance(MockMethodLineHighlighter.class);
    private static final Pattern testFileNamePattern = Pattern.compile("^Test.*V.java$");
    private final MethodMockGutterNavigationHandler methodMockGutterNavigationHandler;

    public MockMethodLineHighlighter() {
        methodMockGutterNavigationHandler = new MethodMockGutterNavigationHandler();
    }

    // method call expressions are in the form
    // <optional qualifier>.<method reference name>( < arguments list > )
    public static boolean isNonStaticDependencyCall(PsiMethodCallExpression methodCall) {
        final PsiExpression qualifier = methodCall.getMethodExpression().getQualifierExpression();
        if (!(qualifier instanceof PsiReferenceExpression) ||
                !(((PsiReferenceExpression) qualifier).resolve() instanceof PsiField)) {
            return false;
        }

        String expressionParentClass = PsiTreeUtil.getParentOfType(
                methodCall, PsiClass.class).getQualifiedName();

        String fieldParentClass = ((PsiClass) ((PsiReferenceExpression) qualifier).resolve()
                .getParent()).getQualifiedName();
        if (!Objects.equals(fieldParentClass, expressionParentClass)) {
            // this field belongs to some other class
            return false;
        }

        // not mocking static calls for now
        PsiMethod targetMethod = (PsiMethod) methodCall.getMethodExpression().getReference().resolve();
        if (targetMethod.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
            return false;
        }

        return true;

    }


    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements, @NotNull Collection<? super LineMarkerInfo<?>> result) {

        final Set<PsiStatement> statements = new HashSet<>();


        for (PsiElement element : elements) {
            ProgressManager.checkCanceled();

            if (element instanceof PsiMethodCallExpression) {
                final PsiMethodCallExpression methodCall = (PsiMethodCallExpression) element;
                final PsiStatement statement = PsiTreeUtil.getParentOfType(methodCall, PsiStatement.class, true,
                        PsiMethod.class);
                if (!statements.contains(statement) && isNonStaticDependencyCall(methodCall)) {
                    statements.add(statement);
                    ContainerUtil.addIfNotNull(result,
                            new LineMarkerInfo<>(
                                    (PsiIdentifier) element.getFirstChild().getLastChild(),
                                    methodCall.getTextRange(), UIUtils.COMPARE_TAB,
                                    psiIdentifier -> "Add mock response",
                                    methodMockGutterNavigationHandler, GutterIconRenderer.Alignment.LEFT));
                }
            }

        }
    }

    public LineMarkerInfo<PsiIdentifier> getLineMarkerInfo(PsiElement element) {
        return null;
    }


}