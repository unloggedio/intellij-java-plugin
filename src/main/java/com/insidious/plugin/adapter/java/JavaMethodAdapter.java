package com.insidious.plugin.adapter.java;

import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.ParameterAdapter;
import com.intellij.debugger.engine.JVMNameUtil;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;

import java.util.Objects;

public class JavaMethodAdapter implements MethodAdapter {
    private final PsiMethod psiMethod;

    public JavaMethodAdapter(PsiMethod methodItem) {
        assert methodItem != null;
        this.psiMethod = methodItem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JavaMethodAdapter that = (JavaMethodAdapter) o;
        return Objects.equals(psiMethod, that.psiMethod);
    }

    @Override
    public int hashCode() {
        return psiMethod.hashCode();
    }

    @Override
    public ClassAdapter getContainingClass() {
        return new JavaClassAdapter(ApplicationManager.getApplication().runReadAction(
                (Computable<PsiClass>) psiMethod::getContainingClass));
    }

    @Override
    public String getName() {
        return psiMethod.getName();
    }

    @Override
    public String getText() {
        return ApplicationManager.getApplication().runReadAction((Computable<String>) psiMethod::getText);
    }

    @Override
    public ParameterAdapter[] getParameters() {
        JvmParameter[] parameters = ApplicationManager.getApplication()
                .runReadAction((Computable<JvmParameter[]>) psiMethod::getParameters);
        ParameterAdapter[] parameterArray = new ParameterAdapter[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            JvmParameter parameter = parameters[i];
            parameterArray[i] = new JavaParameterAdapter(parameter);
        }

        return parameterArray;
    }

    @Override
    public Project getProject() {
        return ApplicationManager.getApplication().runReadAction((Computable<Project>) psiMethod::getProject);
    }

    @Override
    public PsiFile getContainingFile() {
        return psiMethod.getContainingFile();
    }

    @Override
    public boolean isConstructor() {
        return psiMethod.isConstructor();
    }

    @Override
    public PsiType getReturnType() {
        return psiMethod.getReturnType();
    }

    @Override
    public PsiModifierList getModifierList() {
        return psiMethod.getModifierList();
    }

    @Override
    public PsiElement getBody() {
        return psiMethod.getBody();
    }

    @Override
    public String getJVMSignature() {
        return JVMNameUtil.getJVMSignature(psiMethod).toString();
    }

    @Override
    public PsiMethod getPsiMethod() {
        return this.psiMethod;
    }

}
