package com.insidious.plugin.tester.testfinder;

import com.intellij.psi.PsiElement;
import com.intellij.testIntegration.TestFinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

public class UnloggedTestFinder implements TestFinder {
    private static final Logger logger = LoggerFactory.getLogger(UnloggedTestFinder.class);


    @Override
    public @Nullable PsiElement findSourceElement(@NotNull PsiElement from) {
        logger.warn("findSourceElement - {}", from);
        return null;
    }

    @Override
    public @NotNull Collection<PsiElement> findTestsForClass(@NotNull PsiElement element) {
        logger.warn("findTestsForClass - {}", element);
        return Collections.emptyList();
    }

    @Override
    public @NotNull Collection<PsiElement> findClassesForTest(@NotNull PsiElement element) {
        logger.warn("findClassesForTest - {}", element);
        return Collections.emptyList();
    }

    @Override
    public boolean isTest(@NotNull PsiElement element) {
        logger.warn("isTest - {}", element);
        return false;
    }
}
