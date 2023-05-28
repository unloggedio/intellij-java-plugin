package com.insidious.plugin.adapter;

import com.insidious.plugin.agent.AgentCommandRequest;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;

import java.util.List;

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

    PsiMethod getPsiMethod();

}
