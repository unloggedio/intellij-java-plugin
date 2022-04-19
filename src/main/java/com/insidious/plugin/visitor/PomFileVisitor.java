package com.insidious.plugin.visitor;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementVisitor;
import org.jetbrains.annotations.NotNull;

public class PomFileVisitor extends PsiRecursiveElementVisitor {
    private String packageName;
    private Boolean insideParent = false;

    @Override
    public void visitElement(@NotNull PsiElement element) {
        super.visitElement(element);
        if (packageName != null) {
            return;
        }
        if (!insideParent) {
            if (element.textMatches("groupId")) {
                packageName = element.getNextSibling().getNextSibling().getFirstChild().getText();
            }
        }
        if (element.textMatches("parent")) {
            insideParent = !insideParent;
        }
    }

    public String getPackageName() {
        return packageName;
    }
}
