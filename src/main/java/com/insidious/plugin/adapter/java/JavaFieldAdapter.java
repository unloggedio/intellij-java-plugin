package com.insidious.plugin.adapter.java;

import com.insidious.plugin.adapter.FieldAdapter;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiType;

public class JavaFieldAdapter implements FieldAdapter {
    private final PsiField psiField;

    public JavaFieldAdapter(PsiField field) {
        this.psiField = field;
    }

    @Override
    public boolean hasModifier(JvmModifier aStatic) {
        return psiField.hasModifier(aStatic);
    }

    @Override
    public String getName() {
        return psiField.getName();
    }

    @Override
    public PsiType getType() {
        return psiField.getType();
    }
}
