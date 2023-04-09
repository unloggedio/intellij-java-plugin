package com.insidious.plugin.ui.adapter;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.psi.PsiType;

public interface FieldAdapter {
    boolean hasModifier(JvmModifier aStatic);

    String getName();

    PsiType getType();
}
