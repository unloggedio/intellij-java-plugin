package com.insidious.plugin.adapter;

import com.intellij.psi.PsiType;

public interface ParameterAdapter {
    String getName();

    PsiType getType();
}
