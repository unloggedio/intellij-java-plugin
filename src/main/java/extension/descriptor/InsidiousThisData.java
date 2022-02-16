package extension.descriptor;

import com.intellij.debugger.impl.descriptors.data.DescriptorData;
import com.intellij.debugger.impl.descriptors.data.DisplayKey;
import com.intellij.debugger.impl.descriptors.data.SimpleDisplayKey;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

public final class InsidiousThisData extends DescriptorData<InsidiousThisDescriptorImpl> {
    private static final Key THIS = new Key("THIS");


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


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\descriptor\InsidiousThisData.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */