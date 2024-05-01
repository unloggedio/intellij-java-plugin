package com.insidious.plugin.adapter;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
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

    boolean isInterface();

    boolean isEnum();

    boolean isAnnotationType();

    PsiClass getSource();
}
