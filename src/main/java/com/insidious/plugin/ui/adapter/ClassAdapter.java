package com.insidious.plugin.ui.adapter;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.source.PsiJavaFileImpl;

public interface ClassAdapter {

    String getName();

    String getQualifiedName();

    FieldAdapter[] getFields();

    MethodAdapter[] getConstructors();

    Project getProject();

    PsiJavaFileImpl getContainingFile();

    ClassAdapter[] getInterfaces();

    ClassAdapter[] getSupers();

    MethodAdapter[] getMethods();
}
