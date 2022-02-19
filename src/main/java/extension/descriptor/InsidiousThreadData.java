package extension.descriptor;

import com.intellij.debugger.impl.descriptors.data.DescriptorData;
import com.intellij.debugger.impl.descriptors.data.DisplayKey;
import com.intellij.debugger.impl.descriptors.data.SimpleDisplayKey;
import com.intellij.openapi.project.Project;
import extension.thread.InsidiousThreadReferenceProxy;
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
