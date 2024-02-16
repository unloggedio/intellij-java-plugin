package com.insidious.plugin.adapter.java;

import com.insidious.plugin.adapter.FieldAdapter;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiType;

public class JavaFieldAdapter implements FieldAdapter {
    private final PsiField psiField;

    public JavaFieldAdapter(PsiField field) {
        this.psiField = field;
    }

    @Override
    public boolean hasModifier(JvmModifier aStatic) {
        return ApplicationManager.getApplication().runReadAction(
                (Computable<Boolean>) () -> psiField.hasModifier(aStatic));
    }

    @Override
    public String getName() {
        return ApplicationManager.getApplication().runReadAction((Computable<String>) psiField::getName);
    }

    @Override
    public PsiType getType() {
        return ApplicationManager.getApplication().runReadAction((Computable<PsiType>) psiField::getType);
    }

    @Override
    public PsiField getPsiField() {
        return psiField;
    }
}
