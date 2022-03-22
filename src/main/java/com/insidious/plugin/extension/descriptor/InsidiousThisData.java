package com.insidious.plugin.extension.descriptor;

import com.insidious.plugin.extension.thread.types.InsidiousObjectReference;
import com.intellij.debugger.impl.descriptors.data.DescriptorData;
import com.intellij.debugger.impl.descriptors.data.DisplayKey;
import com.intellij.debugger.impl.descriptors.data.SimpleDisplayKey;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

public final class InsidiousThisData extends DescriptorData<InsidiousThisDescriptorImpl> {
    private static final Key THIS = new Key("THIS");
    private InsidiousObjectReference objectReference;

    public InsidiousThisData(InsidiousObjectReference objectReference) {

        this.objectReference = objectReference;
    }




    protected InsidiousThisDescriptorImpl createDescriptorImpl(@NotNull Project project) {
        return new InsidiousThisDescriptorImpl(project);
    }

    public boolean equals(Object object) {
        return object instanceof InsidiousThisData;
    }

    public int hashCode() {
        return THIS.hashCode();
    }


    public DisplayKey<InsidiousThisDescriptorImpl> getDisplayKey() {
        return (DisplayKey<InsidiousThisDescriptorImpl>) new SimpleDisplayKey(THIS);
    }
}

