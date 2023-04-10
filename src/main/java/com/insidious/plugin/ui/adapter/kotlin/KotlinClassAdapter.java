package com.insidious.plugin.ui.adapter.kotlin;

import com.insidious.plugin.ui.adapter.ClassAdapter;
import com.insidious.plugin.ui.adapter.FieldAdapter;
import com.insidious.plugin.ui.adapter.MethodAdapter;
import com.intellij.openapi.project.Project;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import org.jetbrains.kotlin.psi.KtClass;

public class KotlinClassAdapter implements ClassAdapter {
    private final KtClass kotlinClass;

    public KotlinClassAdapter(KtClass kotlinClass) {
        this.kotlinClass = kotlinClass;
    }

    @Override
    public String getName() {
        return kotlinClass.getName();
    }

    @Override
    public String getQualifiedName() {
        return kotlinClass.getFqName().toString();
    }

    @Override
    public FieldAdapter[] getFields() {
        return new FieldAdapter[0];
    }

    @Override
    public MethodAdapter[] getConstructors() {
        return new MethodAdapter[0];
    }

    @Override
    public Project getProject() {
        return null;
    }

    @Override
    public PsiJavaFileImpl getContainingFile() {
        return null;
    }

    @Override
    public ClassAdapter[] getInterfaces() {
        return new ClassAdapter[0];
    }

    @Override
    public ClassAdapter[] getSupers() {
        return new ClassAdapter[0];
    }

    @Override
    public MethodAdapter[] getMethods() {
        return new MethodAdapter[0];
    }
}
