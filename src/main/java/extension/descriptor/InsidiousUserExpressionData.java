package extension.descriptor;

import com.intellij.debugger.engine.evaluation.TextWithImports;
import com.intellij.debugger.impl.descriptors.data.DescriptorData;
import com.intellij.debugger.impl.descriptors.data.DisplayKey;
import com.intellij.debugger.impl.descriptors.data.SimpleDisplayKey;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class InsidiousUserExpressionData
        extends DescriptorData<InsidiousUserExpressionDescriptor> {
    private final InsidiousValueDescriptorImpl myParentDescriptor;
    private final String myTypeName;
    private final String myName;
    protected TextWithImports myText;
    private int myEnumerationIndex = -1;


    public InsidiousUserExpressionData(InsidiousValueDescriptorImpl parentDescriptor, String typeName, String name, TextWithImports text) {
        this.myParentDescriptor = parentDescriptor;
        this.myTypeName = typeName;
        this.myName = name;
        this.myText = text;
    }


    protected InsidiousUserExpressionDescriptorImpl createDescriptorImpl(@NotNull Project project) {
        return new InsidiousUserExpressionDescriptorImpl(project, this.myParentDescriptor, this.myTypeName, this.myName, this.myText, this.myEnumerationIndex);
    }


    public boolean equals(Object object) {
        if (!(object instanceof InsidiousUserExpressionData)) return false;

        return this.myName.equals(((InsidiousUserExpressionData) object).myName);
    }

    public int hashCode() {
        return this.myName.hashCode();
    }


    public DisplayKey<InsidiousUserExpressionDescriptor> getDisplayKey() {
        return (DisplayKey<InsidiousUserExpressionDescriptor>) new SimpleDisplayKey(this.myTypeName + this.myName);
    }

    public void setEnumerationIndex(int enumerationIndex) {
        this.myEnumerationIndex = enumerationIndex;
    }
}

