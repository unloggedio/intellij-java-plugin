package com.insidious.plugin.adapter.java;

import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.adapter.FieldAdapter;
import com.insidious.plugin.adapter.MethodAdapter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
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
        return ApplicationManager.getApplication().runReadAction((Computable<String>) psiClass::getName);
    }

    @Override
    public String getQualifiedName() {
        return ApplicationManager.getApplication().runReadAction((Computable<String>) psiClass::getQualifiedName);
    }

    @Override
    public FieldAdapter[] getFields() {
        PsiField[] fields = ApplicationManager.getApplication().runReadAction(
                (Computable<PsiField[]>) psiClass::getFields);
        FieldAdapter[] fieldsArray = new FieldAdapter[fields.length];
        for (int i = 0; i < fields.length; i++) {
            PsiField field = fields[i];
            fieldsArray[i] = new JavaFieldAdapter(field);
        }

        return fieldsArray;
    }

    @Override
    public MethodAdapter[] getConstructors() {
        PsiMethod[] methodItems = ApplicationManager.getApplication().runReadAction(
                (Computable<PsiMethod[]>) psiClass::getConstructors);
        MethodAdapter[] methodArray = new MethodAdapter[methodItems.length];
        for (int i = 0; i < methodItems.length; i++) {
            PsiMethod methodItem = methodItems[i];
            methodArray[i] = new JavaMethodAdapter(methodItem);
        }

        return methodArray;
    }

    @Override
    public Project getProject() {
        return ApplicationManager.getApplication().runReadAction((Computable<Project>) psiClass::getProject);
    }

    @Override
    public PsiJavaFileImpl getContainingFile() {
        return ApplicationManager.getApplication().runReadAction(
                (Computable<PsiJavaFileImpl>) () -> (PsiJavaFileImpl) psiClass.getContainingFile());
    }

    @Override
    public ClassAdapter[] getInterfaces() {
        PsiClass[] interfacesList = ApplicationManager.getApplication().runReadAction(
                (Computable<PsiClass[]>) psiClass::getInterfaces);
        ClassAdapter[] interfacesArray = new ClassAdapter[interfacesList.length];
        for (int i = 0; i < interfacesList.length; i++) {
            PsiClass aClass = interfacesList[i];
            interfacesArray[i] = new JavaClassAdapter(aClass);
        }

        return interfacesArray;
    }

    @Override
    public ClassAdapter[] getSupers() {
        PsiClass[] interfacesList = ApplicationManager.getApplication().runReadAction(
                (Computable<PsiClass[]>) psiClass::getSupers);
        ClassAdapter[] interfacesArray = new ClassAdapter[interfacesList.length];
        for (int i = 0; i < interfacesList.length; i++) {
            PsiClass aClass = interfacesList[i];
            interfacesArray[i] = new JavaClassAdapter(aClass);
        }

        return interfacesArray;
    }

    @Override
    public MethodAdapter[] getMethods() {
        PsiMethod[] interfacesList = ApplicationManager.getApplication().runReadAction(
                (Computable<PsiMethod[]>) psiClass::getMethods);
        MethodAdapter[] interfacesArray = new MethodAdapter[interfacesList.length];
        for (int i = 0; i < interfacesList.length; i++) {
            PsiMethod aClass = interfacesList[i];
            interfacesArray[i] = new JavaMethodAdapter(aClass);
        }

        return interfacesArray;
    }

    @Override
    public boolean isInterface() {
        return ApplicationManager.getApplication().runReadAction((Computable<Boolean>) psiClass::isInterface);
    }

    @Override
    public boolean isEnum() {
        return ApplicationManager.getApplication().runReadAction((Computable<Boolean>) psiClass::isEnum);
    }

    @Override
    public boolean isAnnotationType() {
        return ApplicationManager.getApplication().runReadAction(
                (Computable<Boolean>) psiClass::isAnnotationType);
    }

    @Override
    public PsiClass getSource() {
        return psiClass;
    }
}
