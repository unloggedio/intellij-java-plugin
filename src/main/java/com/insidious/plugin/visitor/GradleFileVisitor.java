package com.insidious.plugin.visitor;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.PsiWhiteSpace;


public class GradleFileVisitor extends PsiRecursiveElementVisitor {
    private String packageName;
    private Boolean foundGroup = false;

    @Override
    public void visitElement( PsiElement element) {
        super.visitElement(element);
        if (packageName != null) {
            return;
        }
        if (
                foundGroup &&
                        !(element instanceof PsiWhiteSpace) &&
                        !StringUtil.isEmpty(element.getText().trim()) &&
                        !element.getText().equals("group")) {
            packageName = element.getText();
            return;
        }
        if (element.textMatches("group")) {
            foundGroup = true;
        }
    }

    public String getPackageName() {
        return packageName;
    }
}
