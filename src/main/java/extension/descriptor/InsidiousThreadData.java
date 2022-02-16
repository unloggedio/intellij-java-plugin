package extension.descriptor;

import com.intellij.debugger.impl.descriptors.data.DescriptorData;
import com.intellij.debugger.impl.descriptors.data.DisplayKey;
import com.intellij.debugger.impl.descriptors.data.SimpleDisplayKey;
import com.intellij.openapi.project.Project;
import extension.InsidiousThreadReferenceProxy;
import org.jetbrains.annotations.NotNull;

public class InsidiousThreadData extends DescriptorData<InsidiousThreadDescriptorImpl> {
    private final InsidiousThreadReferenceProxy myThread;

    public InsidiousThreadData(InsidiousThreadReferenceProxy thread) {
        this.myThread = thread;
    }


    protected InsidiousThreadDescriptorImpl createDescriptorImpl(@NotNull Project project) {
        return new InsidiousThreadDescriptorImpl(this.myThread);
    }

    public boolean equals(Object object) {
        if (!(object instanceof com.intellij.debugger.impl.descriptors.data.ThreadData)) {
            return false;
        }
        return this.myThread.equals(((InsidiousThreadData) object).myThread);
    }

    public int hashCode() {
        return this.myThread.hashCode();
    }


    public DisplayKey<InsidiousThreadDescriptorImpl> getDisplayKey() {
        return (DisplayKey<InsidiousThreadDescriptorImpl>) new SimpleDisplayKey(this.myThread);
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\descriptor\InsidiousThreadData.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */