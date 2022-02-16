package extension.descriptor.renderer;

import com.intellij.debugger.DebuggerInvocationUtil;
import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.events.DebuggerCommandImpl;
import com.intellij.debugger.engine.jdi.ThreadReferenceProxy;
import com.intellij.debugger.impl.PrioritizedTask;
import com.intellij.debugger.settings.ThreadsViewSettings;
import com.intellij.debugger.ui.impl.tree.TreeBuilder;
import com.intellij.debugger.ui.impl.tree.TreeBuilderNode;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.SpeedSearchComparator;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.frame.XDebuggerTreeNodeHyperlink;
import com.intellij.xdebugger.frame.XValueChildrenList;
import com.intellij.xdebugger.settings.XDebuggerSettingsManager;
import com.sun.jdi.*;
import extension.*;
import extension.connector.InsidiousStackFrameProxy;
import extension.connector.InsidiousThreadReferenceProxyImpl;
import extension.descriptor.*;
import extension.evaluation.EvaluationContext;
import extension.evaluation.EvaluationContextImpl;
import extension.evaluation.InsidiousNodeDescriptorImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

public abstract class InsidiousDebuggerTree extends InsidiousDebuggerTreeBase implements DataProvider {
    public static final DataKey<InsidiousDebuggerTree> DATA_KEY = DataKey.create("DebuggerTree");
    protected static final Key<Rectangle> VISIBLE_RECT = Key.create("VISIBLE_RECT");
    private static final Logger LOG = Logger.getInstance(InsidiousDebuggerTree.class);
    protected final InsidiousNodeManagerImpl myNodeManager;
    protected InsidiousDebugProcess myInsidiousDebugProcess;
    private InsidiousDebuggerTreeNodeImpl myEditedNode;

    public InsidiousDebuggerTree(Project project, InsidiousDebugProcess InsidiousDebugProcess) {
        super(null, project);
        setScrollsOnExpand(false);
        this.myNodeManager = createNodeManager(project);
        this.myInsidiousDebugProcess = InsidiousDebugProcess;

        TreeBuilder model = new TreeBuilder(this) {
            public void buildChildren(TreeBuilderNode node) {
                InsidiousDebuggerTreeNodeImpl debuggerTreeNode = (InsidiousDebuggerTreeNodeImpl) node;

                if (debuggerTreeNode.getDescriptor() instanceof InsidiousDefaultNodeDescriptor) {
                    return;
                }
                InsidiousDebuggerTree.this.buildNode(debuggerTreeNode);
            }


            public boolean isExpandable(TreeBuilderNode builderNode) {
                return InsidiousDebuggerTree.this.isExpandable((InsidiousDebuggerTreeNodeImpl) builderNode);
            }
        };

        model.setRoot(getNodeFactory().getDefaultNode());
        model.addTreeModelListener(new TreeModelListener() {
            public void treeNodesChanged(TreeModelEvent event) {
                InsidiousDebuggerTree.this.hideTooltip();
            }


            public void treeNodesInserted(TreeModelEvent event) {
                InsidiousDebuggerTree.this.hideTooltip();
            }


            public void treeNodesRemoved(TreeModelEvent event) {
                InsidiousDebuggerTree.this.hideTooltip();
            }


            public void treeStructureChanged(TreeModelEvent event) {
                InsidiousDebuggerTree.this.hideTooltip();
            }
        });

        setModel(model);

        TreeSpeedSearch search = new TreeSpeedSearch(this);
        search.setComparator(new SpeedSearchComparator(false));
    }

    protected InsidiousNodeManagerImpl createNodeManager(Project project) {
        return new InsidiousNodeManagerImpl(project, this, this.myInsidiousDebugProcess);
    }


    public void dispose() {
        this.myNodeManager.dispose();
        super.dispose();
    }

    protected boolean isExpandable(InsidiousDebuggerTreeNodeImpl node) {
        InsidiousNodeDescriptorImpl descriptor = node.getDescriptor();
        return descriptor.isExpandable();
    }


    public Object getData(@NotNull String dataId) {
        if (DATA_KEY.is(dataId)) {
            return this;
        }
        return null;
    }

    private void buildNode(InsidiousDebuggerTreeNodeImpl node) {
        if (node == null || node.getDescriptor() == null) {
            return;
        }


        if (this.myInsidiousDebugProcess != null) {
            DebuggerCommandImpl command = getBuildNodeCommand(node);
            if (command != null) {
                node.add(this.myNodeManager.createMessageNode(InsidiousMessageDescriptor.EVALUATING));
                try {
                    command.run();
                } catch (Exception exception) {
                }
            }
        }
    }


    protected DebuggerCommandImpl getBuildNodeCommand(InsidiousDebuggerTreeNodeImpl node) {
        if (node.getDescriptor() instanceof InsidiousStackFrameDescriptorImpl)
            return new BuildStackFrameCommand(node);
        if (node.getDescriptor() instanceof InsidiousValueDescriptorImpl)
            return new BuildValueNodeCommand(node);
        if (node.getDescriptor() instanceof InsidiousStaticDescriptorImpl)
            return new BuildStaticNodeCommand(node);
        if (node.getDescriptor() instanceof InsidiousThreadDescriptorImpl)
            return new BuildThreadCommand(node);
        if (node.getDescriptor() instanceof InsidiousThreadGroupDescriptorImpl) {
            return new BuildThreadGroupCommand(node);
        }
        LOG.assertTrue(false);
        return null;
    }

    public void saveState(InsidiousDebuggerTreeNodeImpl node) {
        if (node.getDescriptor() != null) {
            TreePath path = new TreePath(node.getPath());
            (node.getDescriptor()).myIsExpanded = isExpanded(path);
            (node.getDescriptor()).myIsSelected = getSelectionModel().isPathSelected(path);
            Rectangle rowBounds = getRowBounds(getRowForPath(path));
            if (rowBounds != null && getVisibleRect().contains(rowBounds)) {
                node.getDescriptor().putUserData(VISIBLE_RECT, getVisibleRect());
                (node.getDescriptor()).myIsVisible = true;
            } else {
                node.getDescriptor().putUserData(VISIBLE_RECT, null);
                (node.getDescriptor()).myIsVisible = false;
            }
        }

        for (Enumeration<InsidiousDebuggerTreeNodeImpl> e = node.rawChildren(); e.hasMoreElements(); ) {
            InsidiousDebuggerTreeNodeImpl child = e.nextElement();
            saveState(child);
        }
    }

    public void restoreState(InsidiousDebuggerTreeNodeImpl node) {
        restoreStateImpl(node);
        scrollToVisible(node);
    }

    protected final void scrollToVisible(InsidiousDebuggerTreeNodeImpl scopeNode) {
        TreePath rootPath = new TreePath(scopeNode.getPath());
        int rowCount = getRowCount();
        for (int idx = rowCount - 1; idx >= 0; idx--) {
            TreePath treePath = getPathForRow(idx);
            if (treePath != null &&
                    rootPath.isDescendant(treePath)) {


                InsidiousDebuggerTreeNodeImpl pathNode = (InsidiousDebuggerTreeNodeImpl) treePath.getLastPathComponent();
                InsidiousNodeDescriptorImpl descriptor = pathNode.getDescriptor();

                if (descriptor != null && descriptor.myIsVisible) {
                    Rectangle visibleRect = descriptor.getUserData(VISIBLE_RECT);
                    if (visibleRect != null) {

                        scrollRectToVisible(visibleRect);
                        break;
                    }
                    scrollPathToVisible(treePath);
                    break;
                }
            }
        }
    }


    public void scrollRectToVisible(Rectangle aRect) {
        aRect.width += aRect.x;
        aRect.x = 0;
        super.scrollRectToVisible(aRect);
    }

    private void restoreStateImpl(InsidiousDebuggerTreeNodeImpl node) {
        restoreNodeState(node);
        if ((node.getDescriptor()).myIsExpanded) {
            for (Enumeration<InsidiousDebuggerTreeNodeImpl> e = node.rawChildren(); e.hasMoreElements(); ) {
                InsidiousDebuggerTreeNodeImpl child = e.nextElement();
                restoreStateImpl(child);
            }
        }
    }

    public void restoreState() {
        clearSelection();
        InsidiousDebuggerTreeNodeImpl root = (InsidiousDebuggerTreeNodeImpl) getModel().getRoot();
        if (root != null) {
            restoreState(root);
        }
    }

    protected void restoreNodeState(InsidiousDebuggerTreeNodeImpl node) {
        InsidiousNodeDescriptorImpl descriptor = node.getDescriptor();
        if (descriptor != null) {
            if (node.getParent() == null) {
                descriptor.myIsExpanded = true;
            }

            TreePath path = new TreePath(node.getPath());
            if (descriptor.myIsExpanded) {
                expandPath(path);
            }
            if (descriptor.myIsSelected) {
                addSelectionPath(path);
            }
        }
    }

    public InsidiousNodeManagerImpl getNodeFactory() {
        return this.myNodeManager;
    }

    public TreeBuilder getMutableModel() {
        return (TreeBuilder) getModel();
    }

    public void removeAllChildren() {
        InsidiousDebuggerTreeNodeImpl root = (InsidiousDebuggerTreeNodeImpl) getModel().getRoot();
        root.removeAllChildren();
        treeChanged();
    }

    public void showMessage(InsidiousMessageDescriptor messageDesc) {
        InsidiousDebuggerTreeNodeImpl root = getNodeFactory().getDefaultNode();
        getMutableModel().setRoot(root);
        InsidiousDebuggerTreeNodeImpl message = root.add(messageDesc);
        treeChanged();
        expandPath(new TreePath(message.getPath()));
    }

    public void showMessage(String messageText) {
        showMessage(new InsidiousMessageDescriptor(messageText));
    }

    public final void treeChanged() {
        InsidiousDebuggerTreeNodeImpl node = (InsidiousDebuggerTreeNodeImpl) getModel().getRoot();
        if (node != null) {
            getMutableModel().nodeStructureChanged(node);
            restoreState();
        }
    }


    public void rebuild(XDebugProcess process) {
        if (process == null) {
            return;
        }
        saveState();
        getNodeFactory().setHistoryByContext(process);
        build(process);
    }

    public void saveState() {
        saveState((InsidiousDebuggerTreeNodeImpl) getModel().getRoot());
    }

    public void onEditorShown(InsidiousDebuggerTreeNodeImpl node) {
        this.myEditedNode = node;
        hideTooltip();
    }

    public void onEditorHidden(InsidiousDebuggerTreeNodeImpl node) {
        if (this.myEditedNode != null) {
            assert this.myEditedNode == node;
            this.myEditedNode = null;
        }
    }


    public JComponent createToolTip(MouseEvent e) {
        return (this.myEditedNode != null) ? null : super.createToolTip(e);
    }

    public void hideTooltip() {
        this.myTipManager.hideTooltip();
    }

    protected abstract void build(XDebugProcess paramXDebugProcess);

    public abstract class BuildNodeCommand
            extends DebuggerCommandImpl {
        protected final List<InsidiousDebuggerTreeNodeImpl> myChildren = new LinkedList<>();
        private final InsidiousDebuggerTreeNodeImpl myNode;

        protected BuildNodeCommand(InsidiousDebuggerTreeNodeImpl node) {
            this(node, null);
        }


        protected BuildNodeCommand(InsidiousDebuggerTreeNodeImpl node, ThreadReferenceProxy thread) {
            this.myNode = node;
        }


        public PrioritizedTask.Priority getPriority() {
            return PrioritizedTask.Priority.NORMAL;
        }

        public InsidiousDebuggerTreeNodeImpl getNode() {
            return this.myNode;
        }

        protected void updateUI(boolean scrollToVisible) {
            DebuggerInvocationUtil.swingInvokeLater(getProject(), () -> {
                this.myNode.removeAllChildren();
                for (InsidiousDebuggerTreeNodeImpl debuggerTreeNode : this.myChildren)
                    this.myNode.add(debuggerTreeNode);
                this.myNode.childrenChanged(scrollToVisible);
            });
        }
    }

    protected class BuildStackFrameCommand
            extends BuildNodeCommand {
        public BuildStackFrameCommand(InsidiousDebuggerTreeNodeImpl stackNode) {
            super(stackNode);
        }


        public void action() {
            try {
                InsidiousStaticDescriptorImpl insidiousStaticDescriptorImpl = null;
                InsidiousStackFrameDescriptorImpl stackDescriptor = (InsidiousStackFrameDescriptorImpl) getNode().getDescriptor();
                InsidiousStackFrameProxy frame = stackDescriptor.getFrameProxy();

                Location location = frame.location();
                InsidiousDebuggerTree.LOG.assertTrue((location != null));

                ObjectReference thisObjectReference = frame.getStackFrame().thisObject();
                EvaluationContextImpl evaluationContextImpl = new EvaluationContextImpl(null, frame, thisObjectReference);


                if (thisObjectReference != null) {

                    InsidiousValueDescriptorImpl insidiousValueDescriptorImpl = InsidiousDebuggerTree.this.myNodeManager.getThisDescriptor(
                            stackDescriptor, thisObjectReference);
                } else {

                    insidiousStaticDescriptorImpl = InsidiousDebuggerTree.this.myNodeManager.getStaticDescriptor(stackDescriptor, location
                            .method().declaringType());
                }
                this.myChildren.add(InsidiousDebuggerTree.this.myNodeManager.createNode(insidiousStaticDescriptorImpl, evaluationContextImpl));


                InsidiousClassRenderer classRenderer = RendererManager.getInstance().getClassRenderer();
                if (classRenderer.SHOW_VAL_FIELDS_AS_LOCAL_VARIABLES &&
                        thisObjectReference != null && frame
                        .threadProxy().getVirtualMachine().canGetSyntheticAttribute()) {
                    ReferenceType thisRefType = thisObjectReference.referenceType();
                    if (thisRefType instanceof ClassType && thisRefType
                            .equals(location.declaringType()) && thisRefType

                            .name()
                            .contains("$")) {
                        ClassType clsType = (ClassType) thisRefType;
                        for (Field field : clsType.fields()) {
                            if (DebuggerUtils.isSynthetic(field) &&
                                    StringUtil.startsWith(field
                                            .name(), "val$")) {


                                InsidiousFieldDescriptorImpl fieldDescriptor =
                                        InsidiousDebuggerTree.this.myNodeManager.getFieldDescriptor(stackDescriptor, thisObjectReference, field);

                                this.myChildren.add(InsidiousDebuggerTree.this.myNodeManager
                                        .createNode(fieldDescriptor, evaluationContextImpl));
                            }
                        }
                    }
                }


                try {
                    buildVariables(stackDescriptor, evaluationContextImpl);
                    if (XDebuggerSettingsManager.getInstance()
                            .getDataViewSettings()
                            .isSortValues()) {
                        this.myChildren.sort(InsidiousNodeManagerImpl.getNodeComparator());
                    }
                } catch (EvaluateException e) {
                    this.myChildren.add(InsidiousDebuggerTree.this.myNodeManager
                            .createMessageNode(new InsidiousMessageDescriptor(e
                                    .getMessage())));
                }
            } catch (EvaluateException e) {
                this.myChildren.clear();
                this.myChildren.add(InsidiousDebuggerTree.this.myNodeManager
                        .createMessageNode(new InsidiousMessageDescriptor(e.getMessage())));
            } catch (InvalidStackFrameException e) {
                InsidiousDebuggerTree.LOG.info(e);
                this.myChildren.clear();
                notifyCancelled();
            } catch (InternalException e) {
                if (e.errorCode() == 35) {
                    this.myChildren.add(InsidiousDebuggerTree.this.myNodeManager
                            .createMessageNode(new InsidiousMessageDescriptor(

                                    DebuggerBundle.message("error.corrupt.debug.info", e.getMessage()))));
                } else {
                    throw e;
                }
            }

            updateUI(true);
        }


        protected void buildVariables(InsidiousStackFrameDescriptorImpl stackDescriptor, EvaluationContext evaluationContext) throws EvaluateException {
            InsidiousStackFrameProxy frame = stackDescriptor.getFrameProxy();
            for (InsidiousLocalVariableProxy local : frame.visibleVariables()) {

                InsidiousLocalVariableDescriptorImpl localVariableDescriptor = InsidiousDebuggerTree.this.myNodeManager.getLocalVariableDescriptor(stackDescriptor, local);

                InsidiousDebuggerTreeNodeImpl variableNode = InsidiousDebuggerTree.this.myNodeManager.createNode(localVariableDescriptor, evaluationContext);
                this.myChildren.add(variableNode);
            }
        }
    }

    private class BuildValueNodeCommand
            extends BuildNodeCommand implements InsidiousChildrenBuilder {
        BuildValueNodeCommand(InsidiousDebuggerTreeNodeImpl node) {
            super(node);
        }


        public void action() {
            InsidiousDebuggerTreeNodeImpl node = getNode();
            InsidiousValueDescriptorImpl descriptor = (InsidiousValueDescriptorImpl) node.getDescriptor();
            EvaluationContext evaluationContext = descriptor.getStoredEvaluationContext();
            descriptor
                    .getRenderer()
                    .thenAccept(renderer -> {


                        try {
                            renderer.buildChildren(descriptor.getValue(), this, evaluationContext);
                        } catch (ObjectCollectedException e) {
                            e.printStackTrace();
                            String message = e.getMessage();
//                            DebuggerInvocationUtil.swingInvokeLater(InsidiousDebuggerTree.this.getProject(), ());
                        }
                    });
        }


        public InsidiousNodeManagerImpl getNodeManager() {
            return InsidiousDebuggerTree.this.myNodeManager;
        }


        public InsidiousNodeManagerImpl getDescriptorManager() {
            return InsidiousDebuggerTree.this.myNodeManager;
        }


        public InsidiousValueDescriptorImpl getParentDescriptor() {
            return (InsidiousValueDescriptorImpl) getNode().getDescriptor();
        }


        public void initChildrenArrayRenderer(InsidiousArrayRenderer renderer, int arrayLength) {
        }


        public void setChildren(List<? extends InsidiousDebuggerTreeNode> children) {
            for (InsidiousDebuggerTreeNode child : children) {
                if (child instanceof InsidiousDebuggerTreeNodeImpl) {
                    this.myChildren.add((InsidiousDebuggerTreeNodeImpl) child);
                }
            }
            updateUI(false);
        }

        public void addChildren(@NotNull XValueChildrenList children, boolean last) {
            new Exception().printStackTrace();
        }

        public void tooManyChildren(int remaining) {
        }

        public void setAlreadySorted(boolean alreadySorted) {
        }

        public void setErrorMessage(@NotNull String errorMessage) {
            new Exception().printStackTrace();
        }

        public void setErrorMessage(@NotNull String errorMessage, @Nullable XDebuggerTreeNodeHyperlink link) {
            new Exception().printStackTrace();
        }


        public void setMessage(@NotNull String message, @Nullable Icon icon, @NotNull SimpleTextAttributes attributes, @Nullable XDebuggerTreeNodeHyperlink link) {
        }
    }

    private class BuildStaticNodeCommand extends BuildNodeCommand {
        BuildStaticNodeCommand(InsidiousDebuggerTreeNodeImpl node) {
            super(node);
        }


        public void action() {
            InsidiousStaticDescriptorImpl sd = (InsidiousStaticDescriptorImpl) getNode().getDescriptor();
            ReferenceType refType = sd.getType();
            List<Field> fields = refType.allFields();
            for (Field field : fields) {
                if (field.isStatic()) {

                    InsidiousFieldDescriptorImpl fieldDescriptor = InsidiousDebuggerTree.this.myNodeManager.getFieldDescriptor(sd, null, field);

                    InsidiousDebuggerTreeNodeImpl node = InsidiousDebuggerTree.this.myNodeManager.createNode(fieldDescriptor, null);
                    this.myChildren.add(node);
                }
            }

            updateUI(true);
        }
    }

    private class BuildThreadCommand extends BuildNodeCommand {
        BuildThreadCommand(InsidiousDebuggerTreeNodeImpl threadNode) {
            super(threadNode, ((InsidiousThreadDescriptorImpl) threadNode

                    .getDescriptor()).getThreadReference());
        }


        public void action() {
            InsidiousThreadDescriptorImpl threadDescriptor = (InsidiousThreadDescriptorImpl) getNode().getDescriptor();
            InsidiousThreadReferenceProxy threadProxy = threadDescriptor.getThreadReference();
            if (!threadProxy.isCollected() && threadProxy.getThreadReference().isSuspended()) {
                int status = threadProxy.status();
                if (status != -1 && status != 5 && status != 0) {

                    try {

                        for (InsidiousStackFrameProxy stackFrame : threadProxy.frames()) {


                            EvaluationContextImpl evaluationContextImpl = new EvaluationContextImpl(null, stackFrame, stackFrame.getStackFrame().thisObject());
                            this.myChildren.add(InsidiousDebuggerTree.this.myNodeManager
                                    .createNode(InsidiousDebuggerTree.this.myNodeManager
                                            .getStackFrameDescriptor(threadDescriptor, stackFrame), evaluationContextImpl));
                        }

                    } catch (EvaluateException e) {
                        this.myChildren.clear();
                        this.myChildren.add(InsidiousDebuggerTree.this.myNodeManager.createMessageNode(e.getMessage()));
                        InsidiousDebuggerTree.LOG.debug(e);
                    }
                }
            }


            updateUI(true);
        }
    }

    private class BuildThreadGroupCommand extends DebuggerCommandImpl {
        protected final List<InsidiousDebuggerTreeNodeImpl> myChildren = new LinkedList<>();
        private final InsidiousDebuggerTreeNodeImpl myNode;

        BuildThreadGroupCommand(InsidiousDebuggerTreeNodeImpl node) {
            this.myNode = node;
        }


        protected void action() {
            InsidiousThreadGroupDescriptorImpl groupDescriptor = (InsidiousThreadGroupDescriptorImpl) this.myNode.getDescriptor();
            InsidiousThreadGroupReferenceProxy threadGroup = groupDescriptor.getThreadGroupReference();

            List<InsidiousThreadReferenceProxy> threads = new ArrayList<>(threadGroup.threads());
            threads.sort(InsidiousThreadReferenceProxyImpl.ourComparator);

            boolean showCurrent = (ThreadsViewSettings.getInstance()).SHOW_CURRENT_THREAD;

            for (InsidiousThreadGroupReferenceProxy group : threadGroup.threadGroups()) {
                if (group != null) {

                    InsidiousDebuggerTreeNodeImpl threadNode = InsidiousDebuggerTree.this.myNodeManager.createNode(InsidiousDebuggerTree.this.myNodeManager
                            .getThreadGroupDescriptor(groupDescriptor, group), null);


                    if (showCurrent && ((InsidiousThreadGroupDescriptorImpl) threadNode
                            .getDescriptor())
                            .isCurrent()) {
                        this.myChildren.add(0, threadNode);
                        continue;
                    }
                    this.myChildren.add(threadNode);
                }
            }


            ArrayList<InsidiousDebuggerTreeNodeImpl> threadNodes = new ArrayList<>();

            for (InsidiousThreadReferenceProxy thread : threads) {
                if (thread != null) {

                    InsidiousDebuggerTreeNodeImpl threadNode = InsidiousDebuggerTree.this.myNodeManager.createNode(InsidiousDebuggerTree.this.myNodeManager
                            .getThreadDescriptor(groupDescriptor, thread), null);

                    if (showCurrent && ((InsidiousThreadDescriptorImpl) threadNode
                            .getDescriptor())
                            .isCurrent()) {
                        threadNodes.add(0, threadNode);
                        continue;
                    }
                    threadNodes.add(threadNode);
                }
            }


            this.myChildren.addAll(threadNodes);

            updateUI(true);
        }

        protected void updateUI(boolean scrollToVisible) {
            DebuggerInvocationUtil.swingInvokeLater(InsidiousDebuggerTree.this.getProject(), () -> {
                this.myNode.removeAllChildren();
                for (InsidiousDebuggerTreeNodeImpl debuggerTreeNode : this.myChildren) {
                    this.myNode.add(debuggerTreeNode);
                }
                this.myNode.childrenChanged(scrollToVisible);
            });
        }
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\descriptor\render\InsidiousDebuggerTree.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */