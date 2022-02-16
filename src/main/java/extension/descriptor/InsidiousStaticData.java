package extension.descriptor;

import com.intellij.debugger.impl.descriptors.data.DescriptorData;
import com.intellij.debugger.impl.descriptors.data.DisplayKey;
import com.intellij.debugger.impl.descriptors.data.SimpleDisplayKey;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.sun.jdi.ReferenceType;
import org.jetbrains.annotations.NotNull;

public final class InsidiousStaticData extends DescriptorData<InsidiousStaticDescriptorImpl> {
    private static final Key STATIC = new Key("STATIC");

    private final ReferenceType myRefType;

    public InsidiousStaticData(@NotNull ReferenceType refType) {
        this.myRefType = refType;
    }

    public ReferenceType getRefType() {
        return this.myRefType;
    }


    protected InsidiousStaticDescriptorImpl createDescriptorImpl(@NotNull Project project) {
        return new InsidiousStaticDescriptorImpl(this.myRefType);
    }

    public boolean equals(Object object) {
        return object instanceof InsidiousStaticData;
    }

    public int hashCode() {
        return STATIC.hashCode();
    }


    public DisplayKey<InsidiousStaticDescriptorImpl> getDisplayKey() {
        return (DisplayKey<InsidiousStaticDescriptorImpl>) new SimpleDisplayKey(STATIC);
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\descriptor\InsidiousStaticData.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */