package com.insidious.plugin.visitor;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementVisitor;


public class PomFileVisitor extends PsiRecursiveElementVisitor {
    private String packageName;
    private Boolean insideParent = false;

    @Override
    public void visitElement( PsiElement element) {
        super.visitElement(element);
        if (packageName != null) {
            return;
        }
        if (element.textMatches("groupId")) {
            packageName = element.getNextSibling().getNextSibling().getFirstChild().getText();
            if (!insideParent) {
                if (packageName.startsWith("org.springframework")) {
                    packageName = null;
                }
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
