package com.insidious.plugin.adapter;

import com.insidious.plugin.adapter.java.JavaTypeAdapter;
import com.intellij.psi.PsiType;

public interface ParameterAdapter {
    String getName();

    PsiType getType();
}
