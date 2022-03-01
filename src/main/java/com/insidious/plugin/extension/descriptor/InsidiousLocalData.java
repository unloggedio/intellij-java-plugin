package com.insidious.plugin.extension.descriptor;

import com.intellij.debugger.impl.descriptors.data.DescriptorData;
import com.intellij.debugger.impl.descriptors.data.DisplayKey;
import com.intellij.debugger.impl.descriptors.data.SimpleDisplayKey;
import com.intellij.openapi.project.Project;
import com.insidious.plugin.extension.thread.InsidiousLocalVariableProxy;
import org.jetbrains.annotations.NotNull;

public class InsidiousLocalData extends DescriptorData<InsidiousLocalVariableDescriptorImpl> {
    private final InsidiousLocalVariableProxy myLocalVariable;

    public InsidiousLocalData(InsidiousLocalVariableProxy localVariable) {
        this.myLocalVariable = localVariable;
    }


    protected InsidiousLocalVariableDescriptorImpl createDescriptorImpl(@NotNull Project project) {
        return new InsidiousLocalVariableDescriptorImpl(project, this.myLocalVariable);
    }

    public boolean equals(Object object) {
        if (!(object instanceof InsidiousLocalData)) return false;

        return ((InsidiousLocalData) object).myLocalVariable.equals(this.myLocalVariable);
    }

    public int hashCode() {
        return this.myLocalVariable.hashCode();
    }


    public DisplayKey<InsidiousLocalVariableDescriptorImpl> getDisplayKey() {
        return (DisplayKey<InsidiousLocalVariableDescriptorImpl>) new SimpleDisplayKey(this.myLocalVariable.typeName() + "#" + this.myLocalVariable.name());
    }
}

