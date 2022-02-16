package extension.descriptor.renderer;

import com.intellij.xdebugger.frame.XCompositeNode;
import extension.InsidiousDebuggerTreeNode;
import extension.descriptor.InsidiousNodeDescriptorFactory;
import extension.descriptor.InsidiousValueDescriptor;

import java.util.List;

public interface InsidiousChildrenBuilder extends XCompositeNode {
    InsidiousNodeDescriptorFactory getDescriptorManager();

    InsidiousNodeManager getNodeManager();

    InsidiousValueDescriptor getParentDescriptor();

    void setChildren(List<? extends InsidiousDebuggerTreeNode> paramList);

    default void addChildren(List<? extends InsidiousDebuggerTreeNode> children, boolean last) {
        setChildren(children);
    }


    @Deprecated
    default void setRemaining(int remaining) {
        tooManyChildren(remaining);
    }

    void initChildrenArrayRenderer(InsidiousArrayRenderer paramInsidiousArrayRenderer, int paramInt);
}