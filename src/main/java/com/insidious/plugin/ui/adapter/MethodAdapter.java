package com.insidious.plugin.ui.adapter;

import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;

public interface MethodAdapter {
    ClassAdapter getContainingClass();

    String getName();

    String getText();

    ParameterAdapter[] getParameters();

    Project getProject();

    PsiFile getContainingFile();

    boolean isConstructor();

    PsiType getReturnType();

    PsiModifierList getModifierList();

    PsiElement getBody();

    String getJVMSignature();
}
