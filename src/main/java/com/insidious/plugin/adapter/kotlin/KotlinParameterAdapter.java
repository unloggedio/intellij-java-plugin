package com.insidious.plugin.adapter.kotlin;

import com.insidious.plugin.adapter.ParameterAdapter;
import com.intellij.psi.PsiType;
import org.jetbrains.kotlin.psi.KtParameter;

public class KotlinParameterAdapter implements ParameterAdapter {
    private final KtParameter parameter;

    public KotlinParameterAdapter(KtParameter param) {
        this.parameter = param;
    }

    @Override
    public String getName() {
        return parameter.getName();
    }

    @Override
    public PsiType getType() {
        return null;
    }
}
