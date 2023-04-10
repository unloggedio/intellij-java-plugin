package com.insidious.plugin.ui.adapter.kotlin;

import com.insidious.plugin.ui.adapter.ClassAdapter;
import com.insidious.plugin.ui.adapter.MethodAdapter;
import com.insidious.plugin.ui.adapter.ParameterAdapter;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiType;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtNamedFunction;

public class KotlinMethodAdapter implements MethodAdapter {
    private final KtNamedFunction method;

    public KotlinMethodAdapter(KtNamedFunction method) {
        this.method = method;
    }

    @Override
    public ClassAdapter getContainingClass() {
        KtClass kotlinClass = (KtClass) method.getParent().getParent();
        return new KotlinClassAdapter(kotlinClass);
    }

    @Override
    public String getName() {
        return method.getName();
    }

    @Override
    public String getText() {
        return method.getText();
    }

    @Override
    public ParameterAdapter[] getParameters() {
        return new ParameterAdapter[0];
    }

    @Override
    public Project getProject() {
        return method.getProject();
    }

    @Override
    public PsiFile getContainingFile() {
        return method.getContainingFile();
    }

    @Override
    public boolean isConstructor() {
        return false;
    }

    @Override
    public PsiType getReturnType() {
        return null;
    }

    @Override
    public PsiModifierList getModifierList() {
        return null;
    }

    @Override
    public PsiElement getBody() {
        return method.getBodyExpression();
    }

    @Override
    public String getJVMSignature() {
        return null;
    }

}
