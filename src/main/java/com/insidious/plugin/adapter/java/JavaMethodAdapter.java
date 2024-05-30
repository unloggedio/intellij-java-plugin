package com.insidious.plugin.adapter.java;

import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.ParameterAdapter;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.engine.JVMNameUtil;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class JavaMethodAdapter implements MethodAdapter {
    private final static Logger logger = LoggerUtil.getInstance(JavaMethodAdapter.class);
    private final PsiMethod psiMethod;
    private String cachedSignature;
    private String cachedName;

    public JavaMethodAdapter(PsiMethod methodItem) {
        assert methodItem != null;
        this.psiMethod = methodItem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JavaMethodAdapter that = (JavaMethodAdapter) o;
        return Objects.equals(psiMethod, that.psiMethod);
    }

    @Override
    public int hashCode() {
        return psiMethod.hashCode();
    }

    @Override
    public ClassAdapter getContainingClass() {
        return new JavaClassAdapter(ApplicationManager.getApplication().runReadAction(
                (Computable<PsiClass>) psiMethod::getContainingClass));
    }

    @Override
    public String getName() {
        if (cachedName != null) {
            return cachedName;
        }
        cachedName = ApplicationManager.getApplication().runReadAction((Computable<String>) psiMethod::getName);
        return cachedName;
    }

    @Override
    public String getText() {
        return ApplicationManager.getApplication().runReadAction((Computable<String>) psiMethod::getText);
    }

    @Override
    public ParameterAdapter[] getParameters() {
        JvmParameter[] parameters = ApplicationManager.getApplication()
                .runReadAction((Computable<JvmParameter[]>) psiMethod::getParameters);
        ParameterAdapter[] parameterArray = new ParameterAdapter[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            JvmParameter parameter = parameters[i];
            parameterArray[i] = new JavaParameterAdapter(parameter);
        }

        return parameterArray;
    }

    @Override
    public Project getProject() {
        return ApplicationManager.getApplication().runReadAction((Computable<Project>) psiMethod::getProject);
    }

    @Override
    public PsiFile getContainingFile() {
        return psiMethod.getContainingFile();
    }

    @Override
    public boolean isConstructor() {
        return ApplicationManager.getApplication().runReadAction((Computable<Boolean>) psiMethod::isConstructor);
    }

    @Override
    public PsiType getReturnType() {
        return psiMethod.getReturnType();
    }

    @Override
    public PsiModifierList getModifierList() {
        return psiMethod.getModifierList();
    }

    @Override
    public PsiElement getBody() {
        return psiMethod.getBody();
    }

    @Override
    public String getJVMSignature() {
        if (cachedSignature != null) {
            return cachedSignature;
        }
        cachedSignature = ApplicationManager.getApplication()
                .runReadAction((Computable<String>) () -> {
                    StringBuilder signature = new StringBuilder("(");
                    @NotNull PsiParameterList parameters = psiMethod.getParameterList();
                    int count = parameters.getParametersCount();
                    for (int i = 0; i < count; i++) {
                        PsiParameter param = parameters.getParameter(i);
                        PsiType type = param.getType();
                        appendTypeToSignature(signature, type);
                    }
                    signature.append(")");
                    appendTypeToSignature(signature, psiMethod.getReturnType());
                    return signature.toString();
                });
        return cachedSignature;
    }

    private void appendTypeToSignature(StringBuilder signature, PsiType type) {
        if (type instanceof PsiPrimitiveType) {
            signature.append(JVMNameUtil.getPrimitiveSignature(type.getCanonicalText()));
        } else if (type instanceof PsiArrayType) {
            signature.append("[");
            appendTypeToSignature(signature, ((PsiArrayType) type).getComponentType());
        } else if (type instanceof PsiClassType) {
            PsiClassType classType = (PsiClassType) type;
            int paramCount = classType.getParameterCount();
            signature.append("L").append(classType.rawType().getCanonicalText().replace('.', '/'));
            if (paramCount > 0) {
                signature.append("<");
                for (int i = 0; i < paramCount; i++) {
                    PsiType param = classType.getParameters()[i];
                    appendTypeToSignature(signature, param);
                }
                signature.append(">");
            }
            signature.append(";");
        }
    }

    @Override
    public PsiMethod getPsiMethod() {
        return this.psiMethod;
    }

}
