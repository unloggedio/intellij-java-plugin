package extension.descriptor;

import com.intellij.debugger.engine.jdi.StackFrameProxy;
import com.intellij.debugger.impl.descriptors.data.DescriptorData;
import com.intellij.debugger.impl.descriptors.data.DisplayKey;
import com.intellij.debugger.ui.impl.watch.MethodsTracker;
import com.intellij.openapi.project.Project;
import extension.connector.InsidiousStackFrameProxy;
import extension.descriptor.renderer.InsidiousNodeManagerImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class InsidiousStackFrameData
        extends DescriptorData<InsidiousStackFrameDescriptorImpl> {
    private final InsidiousStackFrameProxy myFrame;
    private final FrameDisplayKey myDisplayKey;
    private final MethodsTracker myMethodsTracker;
    public InsidiousStackFrameData(@NotNull InsidiousStackFrameProxy frame) {
        this.myFrame = frame;
        this

                .myDisplayKey = new FrameDisplayKey(InsidiousNodeManagerImpl.getContextKeyForFrame((StackFrameProxy) frame));
        this.myMethodsTracker = new MethodsTracker();
    }

    protected InsidiousStackFrameDescriptorImpl createDescriptorImpl(@NotNull Project project) {
        return new InsidiousStackFrameDescriptorImpl(this.myFrame, this.myMethodsTracker);
    }

    public boolean equals(Object object) {
        if (!(object instanceof com.intellij.debugger.impl.descriptors.data.StackFrameData)) {
            return false;
        }
        return (((InsidiousStackFrameData) object).myFrame == this.myFrame);
    }

    public int hashCode() {
        return this.myFrame.hashCode();
    }


    public DisplayKey<InsidiousStackFrameDescriptorImpl> getDisplayKey() {
        return this.myDisplayKey;
    }

    private static class FrameDisplayKey implements DisplayKey<InsidiousStackFrameDescriptorImpl> {
        private final String myContextKey;

        FrameDisplayKey(String contextKey) {
            this.myContextKey = contextKey;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FrameDisplayKey that = (FrameDisplayKey) o;

            return Objects.equals(this.myContextKey, that.myContextKey);
        }

        public int hashCode() {
            return (this.myContextKey == null) ? 0 : this.myContextKey.hashCode();
        }
    }
}

