package com.insidious.plugin.extension.descriptor;

import com.intellij.debugger.impl.descriptors.data.DescriptorData;
import com.intellij.debugger.impl.descriptors.data.DisplayKey;
import com.intellij.debugger.impl.descriptors.data.SimpleDisplayKey;
import com.intellij.openapi.project.Project;
import com.insidious.plugin.extension.thread.InsidiousThreadGroupReferenceProxy;
import org.jetbrains.annotations.NotNull;

public class InsidiousThreadGroupData extends DescriptorData<InsidiousThreadGroupDescriptorImpl> {
    private final InsidiousThreadGroupReferenceProxy myThreadGroup;

    public InsidiousThreadGroupData(InsidiousThreadGroupReferenceProxy threadGroup) {
        this.myThreadGroup = threadGroup;
    }


    protected InsidiousThreadGroupDescriptorImpl createDescriptorImpl(@NotNull Project project) {
        return new InsidiousThreadGroupDescriptorImpl(this.myThreadGroup);
    }

    public boolean equals(Object object) {
        if (!(object instanceof com.intellij.debugger.impl.descriptors.data.ThreadGroupData)) {
            return false;
        }
        return this.myThreadGroup.equals(((InsidiousThreadGroupData) object).myThreadGroup);
    }

    public int hashCode() {
        return this.myThreadGroup.hashCode();
    }


    public DisplayKey<InsidiousThreadGroupDescriptorImpl> getDisplayKey() {
        return (DisplayKey<InsidiousThreadGroupDescriptorImpl>) new SimpleDisplayKey(this.myThreadGroup);
    }
}

