package com.insidious.plugin.adapter.kotlin;

import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.ParameterAdapter;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtParameter;

import java.util.List;

public class KotlinMethodAdapter implements MethodAdapter {
    private final KtNamedFunction method;

    public KotlinMethodAdapter(KtNamedFunction method) {
        this.method = method;
    }

    @Override
    public ClassAdapter getContainingClass() {
        KtClass kotlinClass = (KtClass) method.getParent().getParent();
        return new KotlinClassAdapter(kotlinClass);
    }

    @Override
    public String getName() {
        return method.getName();
    }

    @Override
    public String getText() {
        return method.getText();
    }

    @Override
    public ParameterAdapter[] getParameters() {
        List<KtParameter> params = method.getValueParameters();
        ParameterAdapter[] adapterParamsList = new ParameterAdapter[params.size()];
        for (int i = 0; i < params.size(); i++) {
            KtParameter param = params.get(i);
            adapterParamsList[i] = new KotlinParameterAdapter(param);
        }

        return adapterParamsList;
    }

    @Override
    public Project getProject() {
        return method.getProject();
    }

    @Override
    public PsiFile getContainingFile() {
        return method.getContainingFile();
    }

    @Override
    public boolean isConstructor() {
        return false;
    }

    @Override
    public PsiType getReturnType() {
        return null;
    }

    @Override
    public PsiModifierList getModifierList() {
        return null;
    }

    @Override
    public PsiElement getBody() {
        return method.getBodyExpression();
    }

    @Override
    public String getJVMSignature() {
        return null;
    }
    @Override
    public PsiMethod getPsiMethod() {
        return null;
    }

}
