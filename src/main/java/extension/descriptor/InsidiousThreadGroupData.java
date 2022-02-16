package extension.descriptor;

import com.intellij.debugger.impl.descriptors.data.DescriptorData;
import com.intellij.debugger.impl.descriptors.data.DisplayKey;
import com.intellij.debugger.impl.descriptors.data.SimpleDisplayKey;
import com.intellij.openapi.project.Project;
import extension.InsidiousThreadGroupReferenceProxy;
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


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\descriptor\InsidiousThreadGroupData.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */