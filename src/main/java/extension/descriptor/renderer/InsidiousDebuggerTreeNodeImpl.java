package extension.descriptor.renderer;


import com.intellij.debugger.ui.impl.DebuggerTreeRenderer;
import com.intellij.debugger.ui.impl.tree.TreeBuilder;
import com.intellij.debugger.ui.impl.tree.TreeBuilderNode;
import com.intellij.debugger.ui.tree.NodeDescriptor;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.ui.SimpleColoredText;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil;
import com.intellij.xdebugger.impl.ui.tree.ValueMarkup;
import extension.InsidiousDebuggerTreeNode;
import extension.InsidiousJavaDebugProcess;
import extension.descriptor.InsidiousMessageDescriptor;
import extension.descriptor.InsidiousNodeDescriptorProvider;
import extension.descriptor.InsidiousValueDescriptor;
import extension.descriptor.InsidiousValueDescriptorImpl;
import extension.evaluation.InsidiousNodeDescriptorImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.MutableTreeNode;
import java.util.HashMap;
import java.util.Map;

public class InsidiousDebuggerTreeNodeImpl extends TreeBuilderNode implements InsidiousDebuggerTreeNode, InsidiousNodeDescriptorProvider, MutableTreeNode {
    private final Map myProperties = new HashMap<>();
    private final InsidiousDebuggerTree myTree;
    private final XDebugProcess myProcess;
    private Icon myIcon;
    private SimpleColoredText myText;
    private String myMarkupTooltipText;

    public InsidiousDebuggerTreeNodeImpl(InsidiousDebuggerTree tree, NodeDescriptor descriptor, InsidiousJavaDebugProcess InsidiousJavaDebugProcess) {
        super(descriptor);
        this.myTree = tree;
        this.myProcess = InsidiousJavaDebugProcess;
    }

    private static void invoke(Runnable r) {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    @NotNull
    public static InsidiousDebuggerTreeNodeImpl createNodeNoUpdate(InsidiousDebuggerTree tree, NodeDescriptor descriptor, InsidiousJavaDebugProcess InsidiousJavaDebugProcess) {
        InsidiousDebuggerTreeNodeImpl node = new InsidiousDebuggerTreeNodeImpl(tree, descriptor, InsidiousJavaDebugProcess);

        node.updateCaches();
        return node;
    }

    @NotNull
    protected static InsidiousDebuggerTreeNodeImpl createNode(InsidiousDebuggerTree tree, InsidiousNodeDescriptorImpl descriptor, InsidiousJavaDebugProcess InsidiousJavaDebugProcess) {
        final InsidiousDebuggerTreeNodeImpl node = new InsidiousDebuggerTreeNodeImpl(tree, descriptor, InsidiousJavaDebugProcess);

        descriptor.updateRepresentationNoNotify(node.myProcess, () -> {
            node.updateCaches();
            node.labelChanged();
        });
        node.updateCaches();
        return node;
    }

    public InsidiousDebuggerTreeNodeImpl getParent() {
        return (InsidiousDebuggerTreeNodeImpl) super.getParent();
    }

    protected TreeBuilder getTreeBuilder() {
        return this.myTree.getMutableModel();
    }

    public InsidiousDebuggerTree getTree() {
        return this.myTree;
    }

    public String toString() {
        return (this.myText != null) ? this.myText.toString() : "";
    }

    public InsidiousNodeDescriptorImpl getDescriptor() {
        return (InsidiousNodeDescriptorImpl) getUserObject();
    }

    public Project getProject() {
        return getTree().getProject();
    }

    public void setRenderer(InsidiousNodeRenderer renderer) {
        ((InsidiousValueDescriptorImpl) getDescriptor()).setRenderer(renderer);
        calcRepresentation();
    }

    private void updateCaches() {
        InsidiousNodeDescriptorImpl descriptor = getDescriptor();
        this.myIcon = DebuggerTreeRenderer.getDescriptorIcon(descriptor);
        this.myText = InsidiousDebuggerTreeRenderer.getDescriptorText(
                this.myProcess, descriptor, DebuggerUIUtil.getColorScheme(this.myTree), false);
        if (descriptor instanceof InsidiousValueDescriptor) {
            ValueMarkup markup = ((InsidiousValueDescriptor) descriptor).getMarkup(this.myProcess);
            this.myMarkupTooltipText = (markup != null) ? markup.getToolTipText() : null;
        } else {
            this.myMarkupTooltipText = null;
        }
    }

    public Icon getIcon() {
        return this.myIcon;
    }

    public SimpleColoredText getText() {
        return this.myText;
    }

    @Nullable
    public String getMarkupTooltipText() {
        return this.myMarkupTooltipText;
    }

    public void clear() {
        removeAllChildren();
        this.myIcon = null;
        this.myText = null;
        super.clear();
    }

    private void update(XDebugProcess process, Runnable runnable, boolean labelOnly) {
        if (!labelOnly) {
            clear();
        }

        if (process != null) {
            getTree().saveState(this);

            this.myIcon = InsidiousDebuggerTreeRenderer.getDescriptorIcon(InsidiousMessageDescriptor.EVALUATING);
            this
                    .myText = InsidiousDebuggerTreeRenderer.getDescriptorText(process, (InsidiousNodeDescriptorImpl) InsidiousMessageDescriptor.EVALUATING, false);

            runnable.run();
        }

        labelChanged();
        if (!labelOnly) {
            childrenChanged(true);
        }
    }

    public void calcLabel() {
        update(this.myProcess, () -> getDescriptor().updateRepresentation(this.myProcess, new DescriptorLabelListener() {


            public void labelChanged() {
                InsidiousDebuggerTreeNodeImpl.this.updateCaches();
                InsidiousDebuggerTreeNodeImpl.this.labelChanged();
            }
        }), true);
    }

    public void calcRepresentation() {
        update(this.myProcess, () -> getDescriptor().updateRepresentation(this.myProcess, new DescriptorLabelListener() {


            public void labelChanged() {
                InsidiousDebuggerTreeNodeImpl.this.updateCaches();
                InsidiousDebuggerTreeNodeImpl.this.labelChanged();
            }
        }), false);
    }

    public void calcValue() {
        update(this.myProcess, () -> {
            getDescriptor().updateRepresentation(this.myProcess, new DescriptorLabelListener() {


                public void labelChanged() {
                    InsidiousDebuggerTreeNodeImpl.this.updateCaches();
                    InsidiousDebuggerTreeNodeImpl.this.labelChanged();
                }
            });
            childrenChanged(true);
        }, false);
    }

    public void labelChanged() {
        invoke(() -> {
            updateCaches();
            getTree().getMutableModel().nodeChanged(this);
        });
    }

    public void childrenChanged(boolean scrollToVisible) {
        invoke(() -> {
            getTree().getMutableModel().nodeStructureChanged(this);
            getTree().restoreState(this);
        });
    }

    public InsidiousDebuggerTreeNodeImpl add(InsidiousMessageDescriptor message) {
        InsidiousDebuggerTreeNodeImpl node = getNodeFactory().createMessageNode(message);
        add(node);
        return node;
    }

    public InsidiousNodeManagerImpl getNodeFactory() {
        return this.myTree.getNodeFactory();
    }

    public Object getProperty(Key key) {
        return this.myProperties.get(key);
    }

    public void putProperty(Key key, Object data) {
        this.myProperties.put(key, data);
    }
}

