package com.insidious.plugin.adapter;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiType;

public interface FieldAdapter {
    boolean hasModifier(JvmModifier aStatic);

    String getName();

    PsiType getType();

    PsiField getPsiField();
}
