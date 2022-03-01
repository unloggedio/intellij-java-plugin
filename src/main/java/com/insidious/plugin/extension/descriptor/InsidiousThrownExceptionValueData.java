package com.insidious.plugin.extension.descriptor;

import com.intellij.debugger.impl.descriptors.data.DescriptorData;
import com.intellij.debugger.impl.descriptors.data.DisplayKey;
import com.intellij.debugger.impl.descriptors.data.SimpleDisplayKey;
import com.intellij.openapi.project.Project;
import com.sun.jdi.ObjectReference;
import org.jetbrains.annotations.NotNull;

public final class InsidiousThrownExceptionValueData
        extends DescriptorData<InsidiousThrownExceptionValueDescriptorImpl> {
    @NotNull
    private final ObjectReference myExceptionObj;

    public InsidiousThrownExceptionValueData(@NotNull ObjectReference exceptionObj) {
        this.myExceptionObj = exceptionObj;
    }

    protected InsidiousThrownExceptionValueDescriptorImpl createDescriptorImpl(@NotNull Project project) {
        return new InsidiousThrownExceptionValueDescriptorImpl(project, this.myExceptionObj);
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InsidiousThrownExceptionValueData data = (InsidiousThrownExceptionValueData) o;

        return this.myExceptionObj.equals(data.myExceptionObj);
    }


    public int hashCode() {
        return this.myExceptionObj.hashCode();
    }


    public DisplayKey<InsidiousThrownExceptionValueDescriptorImpl> getDisplayKey() {
        return (DisplayKey<InsidiousThrownExceptionValueDescriptorImpl>) new SimpleDisplayKey(this.myExceptionObj);
    }
}
