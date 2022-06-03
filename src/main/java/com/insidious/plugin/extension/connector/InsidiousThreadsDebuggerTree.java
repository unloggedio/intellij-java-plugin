package com.insidious.plugin.extension.connector;

import com.insidious.plugin.extension.InsidiousJavaDebugProcess;
import com.insidious.plugin.extension.descriptor.InsidiousMessageDescriptor;
import com.insidious.plugin.extension.descriptor.InsidiousStackFrameDescriptorImpl;
import com.insidious.plugin.extension.descriptor.InsidiousThreadDescriptorImpl;
import com.insidious.plugin.extension.descriptor.InsidiousThreadGroupDescriptorImpl;
import com.insidious.plugin.extension.descriptor.renderer.InsidiousDebuggerTree;
import com.insidious.plugin.extension.descriptor.renderer.InsidiousDebuggerTreeNodeImpl;
import com.insidious.plugin.extension.descriptor.renderer.InsidiousNodeManagerImpl;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import com.insidious.plugin.extension.evaluation.InsidiousNodeDescriptorImpl;
import com.insidious.plugin.extension.thread.InsidiousThreadGroupReferenceProxy;
import com.insidious.plugin.extension.thread.InsidiousThreadReferenceProxy;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.DebuggerInvocationUtil;
import com.intellij.debugger.engine.events.DebuggerCommandImpl;
import com.intellij.debugger.engine.jdi.StackFrameProxy;
import com.intellij.debugger.engine.jdi.ThreadReferenceProxy;
import com.intellij.debugger.jdi.ThreadGroupReferenceProxyImpl;
import com.intellij.debugger.settings.ThreadsViewSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.tree.TreeModelAdapter;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebuggerBundle;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class InsidiousThreadsDebuggerTree extends InsidiousDebuggerTree {
    private static final Logger logger = LoggerUtil.getInstance(InsidiousThreadsDebuggerTree.class);

    public InsidiousThreadsDebuggerTree(Project project, InsidiousJavaDebugProcess InsidiousJavaDebugProcess) {
        super(project, InsidiousJavaDebugProcess);
        getEmptyText().setText(XDebuggerBundle.message("debugger.threads.not.available"));
    }


    protected InsidiousNodeManagerImpl createNodeManager(Project project) {
        return new InsidiousNodeManagerImpl(project, this, this.myInsidiousJavaDebugProcess) {
            public String getContextKey(StackFrameProxy frame) {
                return "ThreadsView";
            }
        };
    }


    protected boolean isExpandable(InsidiousDebuggerTreeNodeImpl node) {
        InsidiousNodeDescriptorImpl descriptor = node.getDescriptor();
        if (descriptor instanceof InsidiousStackFrameDescriptorImpl) {
            return false;
        }
        return descriptor.isExpandable();
    }


    protected void build(XDebugProcess process) {
        RefreshThreadsTreeCommand command = new RefreshThreadsTreeCommand(process);

        showMessage(InsidiousMessageDescriptor.EVALUATING);
        command.action();
    }

    private class RefreshThreadsTreeCommand extends DebuggerCommandImpl {
        private final XDebugProcess myProcess;

        RefreshThreadsTreeCommand(XDebugProcess process) {
            this.myProcess = process;
        }


        protected void action() {
            InsidiousDebuggerTreeNodeImpl root = getNodeFactory().getDefaultNode();

            if (this.myProcess == null || this.myProcess.getSession() == null) {
                return;
            }

            boolean showGroups = (ThreadsViewSettings.getInstance()).SHOW_THREAD_GROUPS;

            try {
                InsidiousJavaDebugProcess InsidiousJavaDebugProcess = (InsidiousJavaDebugProcess) this.myProcess.getSession().getDebugProcess();
                InsidiousThreadReferenceProxy currentThread = null;
                InsidiousJDIConnector insidiousJDIConnector = InsidiousJavaDebugProcess.getConnector();
                EvaluationContext evaluationContext = null;
                InsidiousNodeManagerImpl nodeManager = getNodeFactory();

                if (showGroups) {
                    ThreadGroupReferenceProxyImpl topCurrentGroup = null;

                    for (InsidiousThreadGroupReferenceProxy group : insidiousJDIConnector.topLevelThreadGroups()) {
                        if (group != topCurrentGroup) {

                            InsidiousDebuggerTreeNodeImpl threadGroup = nodeManager.createNode(
                                    nodeManager.getThreadGroupDescriptor(null, group), evaluationContext);

                            root.add(threadGroup);
                        }
                    }
                } else {

                    if (currentThread != null) {
                        root.insert(nodeManager
                                .createNode(nodeManager
                                        .getThreadDescriptor(null, currentThread), evaluationContext), 0);
                    }


                    List<InsidiousThreadReferenceProxy> allThreads = new ArrayList<>(insidiousJDIConnector.allThreads());
                    allThreads.sort(InsidiousThreadReferenceProxyImpl.ourComparator);

                    for (InsidiousThreadReferenceProxy threadProxy : allThreads) {
                        if (threadProxy.equals(currentThread)) {
                            continue;
                        }
                        root.add(nodeManager
                                .createNode(nodeManager
                                        .getThreadDescriptor(null, threadProxy), evaluationContext));
                    }

                }
            } catch (Exception ex) {
                root.add(InsidiousMessageDescriptor.DEBUG_INFO_UNAVAILABLE);
                logger.debug("failed", ex);
            }

            DebuggerInvocationUtil.swingInvokeLater(getProject(), () -> {
                getMutableModel().setRoot(root);
                treeChanged();
            });
        }


        private void selectThread(final List<ThreadGroupReferenceProxyImpl> pathToThread, final ThreadReferenceProxy thread, final boolean expand) {
            logger.info("is event dispatch thread - " + SwingUtilities.isEventDispatchThread());
            class MyTreeModelAdapter extends TreeModelAdapter {
                private void structureChanged(InsidiousDebuggerTreeNodeImpl node) {
                    Enumeration<? extends TreeNode> enumeration = node.children();
                    while (enumeration.hasMoreElements()) {

                        InsidiousDebuggerTreeNodeImpl child = (InsidiousDebuggerTreeNodeImpl) enumeration.nextElement();
                        nodeChanged(child);
                    }
                }

                private void nodeChanged(InsidiousDebuggerTreeNodeImpl debuggerTreeNode) {
                    if (pathToThread.size() == 0) {
                        if (debuggerTreeNode.getDescriptor() instanceof InsidiousThreadDescriptorImpl
                                && ((InsidiousThreadDescriptorImpl) debuggerTreeNode
                                .getDescriptor())
                                .getThreadReference() == thread) {
                            removeListener();
                            TreePath treePath = new TreePath(debuggerTreeNode.getPath());
                            setSelectionPath(treePath);
                            if (expand && !isExpanded(treePath)) {
                                expandPath(treePath);
                            }
                        }

                    } else if (debuggerTreeNode.getDescriptor() instanceof InsidiousThreadGroupDescriptorImpl
                            && ((InsidiousThreadGroupDescriptorImpl) debuggerTreeNode


                            .getDescriptor())
                            .getThreadGroupReference() == pathToThread
                            .get(0)) {
                        pathToThread.remove(0);
                        expandPath(new TreePath(debuggerTreeNode.getPath()));
                    }
                }


                private void removeListener() {
                    TreeModelAdapter listener = this;
                    ApplicationManager.getApplication()
                            .invokeLater(() -> getModel().removeTreeModelListener(listener));
                }


                public void treeStructureChanged(TreeModelEvent event) {
                    if ((event.getPath()).length <= 1) {
                        removeListener();
                        return;
                    }
                    structureChanged((InsidiousDebuggerTreeNodeImpl) event
                            .getTreePath().getLastPathComponent());
                }
            }

            MyTreeModelAdapter listener = new MyTreeModelAdapter();
//            MyTreeModelAdapter.access$100(listener, (InsidiousDebuggerTreeNodeImpl) getModel().getRoot());
            getModel().addTreeModelListener(listener);
        }
    }
}

