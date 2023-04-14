package com.insidious.plugin.adapter.java;

import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.adapter.FieldAdapter;
import com.insidious.plugin.adapter.MethodAdapter;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.source.PsiJavaFileImpl;

public class JavaClassAdapter implements ClassAdapter {

    private final PsiClass psiClass;

    public JavaClassAdapter(PsiClass psiClass) {
        this.psiClass = psiClass;
    }

    @Override
    public String getName() {
        return psiClass.getName();
    }

    @Override
    public String getQualifiedName() {
        return psiClass.getQualifiedName();
    }

    @Override
    public FieldAdapter[] getFields() {
        PsiField[] fields = psiClass.getFields();
        FieldAdapter[] fieldsArray = new FieldAdapter[fields.length];
        for (int i = 0; i < fields.length; i++) {
            PsiField field = fields[i];
            fieldsArray[i] = new JavaFieldAdapter(field);
        }

        return fieldsArray;
    }

    @Override
    public MethodAdapter[] getConstructors() {
        PsiMethod[] methodItems = psiClass.getConstructors();
        MethodAdapter[] methodArray = new MethodAdapter[methodItems.length];
        for (int i = 0; i < methodItems.length; i++) {
            PsiMethod methodItem = methodItems[i];
            methodArray[i] = new JavaMethodAdapter(methodItem);
        }

        return methodArray;
    }

    @Override
    public Project getProject() {
        return psiClass.getProject();
    }

    @Override
    public PsiJavaFileImpl getContainingFile() {
        return (PsiJavaFileImpl) psiClass.getContainingFile();
    }

    @Override
    public ClassAdapter[] getInterfaces() {
        PsiClass[] interfacesList = psiClass.getInterfaces();
        ClassAdapter[] interfacesArray = new ClassAdapter[interfacesList.length];
        for (int i = 0; i < interfacesList.length; i++) {
            PsiClass aClass = interfacesList[i];
            interfacesArray[i] = new JavaClassAdapter(aClass);
        }

        return interfacesArray;
    }

    @Override
    public ClassAdapter[] getSupers() {
        PsiClass[] interfacesList = psiClass.getSupers();
        ClassAdapter[] interfacesArray = new ClassAdapter[interfacesList.length];
        for (int i = 0; i < interfacesList.length; i++) {
            PsiClass aClass = interfacesList[i];
            interfacesArray[i] = new JavaClassAdapter(aClass);
        }

        return interfacesArray;
    }

    @Override
    public MethodAdapter[] getMethods() {
        PsiMethod[] interfacesList = psiClass.getMethods();
        MethodAdapter[] interfacesArray = new MethodAdapter[interfacesList.length];
        for (int i = 0; i < interfacesList.length; i++) {
            PsiMethod aClass = interfacesList[i];
            interfacesArray[i] = new JavaMethodAdapter(aClass);
        }

        return interfacesArray;
    }
}
