package com.insidious.plugin.ui.adapter.java;

import com.insidious.plugin.ui.adapter.ParameterAdapter;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.psi.PsiType;

public class JavaParameterAdapter implements ParameterAdapter {
    private final JvmParameter jvmParameters;

    public JavaParameterAdapter(JvmParameter parameter) {
        this.jvmParameters = parameter;
    }

    @Override
    public String getName() {
        return jvmParameters.getName();
    }

    @Override
    public PsiType getType() {
        return (PsiType) jvmParameters.getType();
    }
}
