package com.insidious.plugin.adapter.java;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiType;

public class JavaTypeAdapter {
    private final PsiType type;

    public JavaTypeAdapter(PsiType psiType) {
        this.type = psiType;
    }

    public String getCanonicalText() {
        return ApplicationManager.getApplication().runReadAction((Computable<String>) type::getCanonicalText);
    }
}
