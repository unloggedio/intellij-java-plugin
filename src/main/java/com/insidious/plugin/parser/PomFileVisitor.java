package com.insidious.plugin.parser;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementVisitor;
import org.jetbrains.annotations.NotNull;

public class PomFileVisitor extends PsiRecursiveElementVisitor {
    private String packageName;

    @Override
    public void visitElement(@NotNull PsiElement element) {
        super.visitElement(element);
        if (packageName != null) {
            return;
        }
        if (element.textMatches("groupId")) {
            packageName = element.getNextSibling().getNextSibling().getFirstChild().getText();
        }
    }

    public String getPackageName() {
        return packageName;
    }
}
