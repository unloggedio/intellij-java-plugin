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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MockMethodLineHighlighter implements LineMarkerProvider {

    private static final Logger logger = LoggerUtil.getInstance(MockMethodLineHighlighter.class);
    private static final Pattern testFileNamePattern = Pattern.compile("^Test.*V.java$");
    private static final Pattern testPreviewFilePattern = Pattern.compile("^.*_Unlogged_Preview.java$");
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

        PsiClass parentClass = PsiTreeUtil.getParentOfType(methodCall, PsiClass.class);
        if (parentClass == null) {
            logger.warn("parent class is null [" + methodCall.getText() + " ]");
            return false;
        }
        String expressionParentClass = parentClass.getQualifiedName();

        PsiClass fieldParentPsiClass = (PsiClass) ((PsiReferenceExpression) qualifier).resolve()
                .getParent();
        String fieldParentClass = fieldParentPsiClass.getQualifiedName();
        if (!Objects.equals(fieldParentClass, expressionParentClass) &&
                !IsImplementedBy(fieldParentPsiClass, parentClass)) {
            // this field belongs to some other class
            return false;
        }

        // not mocking static calls for now
        PsiMethod targetMethod = (PsiMethod) methodCall.getMethodExpression().getReference().resolve();
        if (targetMethod == null) {
            logger.warn("Failed to resolve target method call: " + methodCall.getText());
            return false;
        }
        PsiModifierList modifierList = targetMethod.getModifierList();
        if (modifierList.hasModifierProperty(PsiModifier.STATIC)) {
            return false;
        }

        return true;

    }

    // method call expressions are in the form
    // <optional qualifier>.<method reference name>( < arguments list > )
    public static boolean isNonStaticDependencyCall(PsiMethod targetMethod) {

        // not mocking static calls for now
        if (targetMethod == null) {
            logger.warn("Failed to resolve target method call: " + targetMethod.getText());
            return false;
        }
        PsiModifierList modifierList = targetMethod.getModifierList();
        if (modifierList.hasModifierProperty(PsiModifier.STATIC)) {
            return false;
        }

        return true;

    }

    public static boolean isNonStaticDependencyCall(PsiMethodReferenceExpression methodCall) {
        final PsiExpression qualifier = methodCall.getQualifierExpression();
        if (!(qualifier instanceof PsiReferenceExpression) ||
                !(((PsiReferenceExpression) qualifier).resolve() instanceof PsiField)) {
            return false;
        }

        PsiClass parentClass = PsiTreeUtil.getParentOfType(methodCall, PsiClass.class);
        if (parentClass == null) {
            logger.warn("parent class is null [" + methodCall.getText() + " ]");
            return false;
        }
        String expressionParentClass = parentClass.getQualifiedName();

        PsiClass fieldParentPsiClass = (PsiClass) ((PsiReferenceExpression) qualifier).resolve()
                .getParent();
        String fieldParentClass = fieldParentPsiClass.getQualifiedName();
        if (!Objects.equals(fieldParentClass, expressionParentClass) &&
                !IsImplementedBy(fieldParentPsiClass, parentClass)) {
            // this field belongs to some other class
            return false;
        }

        // not mocking static calls for now
        PsiMethod targetMethod = (PsiMethod) methodCall.getReference().resolve();
        if (targetMethod == null) {
            logger.warn("Failed to resolve target method call: " + methodCall.getText());
            return false;
        }
        PsiModifierList modifierList = targetMethod.getModifierList();
        if (modifierList.hasModifierProperty(PsiModifier.STATIC)) {
            return false;
        }

        return true;

    }

    private static boolean IsImplementedBy(PsiClass topClass, PsiClass bottomClass) {
        if (bottomClass == null || bottomClass.getQualifiedName() == null) {
            return false;
        }
        if (bottomClass.getQualifiedName().equals(topClass.getQualifiedName())) {
            return true;
        }
        if (bottomClass.getImplementsList() != null) {
            for (PsiClassType referencedType : bottomClass.getImplementsList().getReferencedTypes()) {
                if (IsImplementedBy(topClass, referencedType.resolve())) {
                    return true;
                }
            }
        }

        return IsImplementedBy(topClass, bottomClass.getSuperClass());
    }


    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements, @NotNull Collection<? super LineMarkerInfo<?>> result) {

        final Set<PsiStatement> statements = new HashSet<>();

        for (PsiElement element : elements) {
            ProgressManager.checkCanceled();
            Matcher previewFileMatcher = testPreviewFilePattern.matcher(element.getContainingFile().getName());
            if (previewFileMatcher.matches()) {
                continue;
            }
//            logger.warn("element [" + element.getClass().getName() + "] => " + element);
            if (element instanceof PsiMethodReferenceExpression) {
                final PsiMethodReferenceExpression methodCall = (PsiMethodReferenceExpression) element;
                final PsiStatement statement = PsiTreeUtil.getParentOfType(methodCall, PsiStatement.class, true,
                        PsiMethod.class);
                if (!statements.contains(statement) && isNonStaticDependencyCall(methodCall)) {
                    statements.add(statement);
                    ContainerUtil.addIfNotNull(result,
                            new LineMarkerInfo<>(
                                    (PsiIdentifier) element.getFirstChild().getLastChild(),
                                    methodCall.getTextRange(), UIUtils.GHOST_MOCK,
                                    psiIdentifier -> "Add mock response",
                                    methodMockGutterNavigationHandler, GutterIconRenderer.Alignment.LEFT));
                }
            }
            if (element instanceof PsiMethodCallExpression) {
                final PsiMethodCallExpression methodCall = (PsiMethodCallExpression) element;
                final PsiStatement statement = PsiTreeUtil.getParentOfType(methodCall, PsiStatement.class, true,
                        PsiMethod.class);
                if (!statements.contains(statement) && isNonStaticDependencyCall(methodCall)) {
                    statements.add(statement);
                    ContainerUtil.addIfNotNull(result,
                            new LineMarkerInfo<>(
                                    (PsiIdentifier) element.getFirstChild().getLastChild(),
                                    methodCall.getTextRange(), UIUtils.GHOST_MOCK,
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