package com.insidious.plugin.adapter.java;

import com.insidious.plugin.adapter.ParameterAdapter;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiType;

public class JavaParameterAdapter implements ParameterAdapter {
    private final JvmParameter jvmParameters;

    public JavaParameterAdapter(JvmParameter parameter) {
        this.jvmParameters = parameter;
    }

    @Override
    public String getName() {
        return ApplicationManager.getApplication().runReadAction((Computable<String>) jvmParameters::getName);
    }

    @Override
    public PsiType getType() {
        return ApplicationManager.getApplication().runReadAction(
                (Computable<PsiType>) () -> (PsiType) jvmParameters.getType());
    }
}
