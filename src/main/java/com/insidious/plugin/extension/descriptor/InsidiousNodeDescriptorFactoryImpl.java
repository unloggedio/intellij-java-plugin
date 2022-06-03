package com.insidious.plugin.extension.descriptor;

import com.insidious.plugin.extension.thread.types.InsidiousObjectReference;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.engine.StackFrameContext;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.TextWithImports;
import com.intellij.debugger.engine.jdi.StackFrameProxy;
import com.intellij.debugger.engine.jdi.ThreadReferenceProxy;
import com.intellij.debugger.impl.descriptors.data.DescriptorData;
import com.intellij.debugger.impl.descriptors.data.DescriptorKey;
import com.intellij.debugger.impl.descriptors.data.DisplayKey;
import com.intellij.debugger.ui.impl.watch.DescriptorTree;
import com.intellij.debugger.ui.impl.watch.MarkedDescriptorTree;
import com.intellij.debugger.ui.tree.NodeDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.sun.jdi.*;
import com.insidious.plugin.extension.thread.InsidiousLocalVariableProxy;
import com.insidious.plugin.extension.thread.InsidiousThreadGroupReferenceProxy;
import com.insidious.plugin.extension.thread.InsidiousThreadReferenceProxy;
import com.insidious.plugin.extension.connector.DecompiledLocalVariable;
import com.insidious.plugin.extension.connector.InsidiousStackFrameProxy;
import com.insidious.plugin.extension.evaluation.InsidiousNodeDescriptorImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class InsidiousNodeDescriptorFactoryImpl implements InsidiousNodeDescriptorFactory {
    private static final Logger logger = LoggerUtil.getInstance(InsidiousNodeDescriptorFactoryImpl.class);
    protected final Project myProject;
    private DescriptorTree myCurrentHistoryTree = new DescriptorTree(true);
    private DescriptorTreeSearcher myDescriptorSearcher;
    private DescriptorTreeSearcher myDisplayDescriptorSearcher;

    public InsidiousNodeDescriptorFactoryImpl(Project project) {
        this.myProject = project;
        this.myDescriptorSearcher = new DescriptorTreeSearcher(new MarkedDescriptorTree());


        this.myDisplayDescriptorSearcher = new DisplayDescriptorTreeSearcher(new MarkedDescriptorTree());
    }

    private static DescriptorTree createDescriptorTree(StackFrameProxy frameProxy, DescriptorTree fromTree) {
        int frameCount = -1;
        int frameIndex = -1;
        if (frameProxy != null) {
            try {
                ThreadReferenceProxy threadReferenceProxy = frameProxy.threadProxy();
                frameCount = threadReferenceProxy.frameCount();
                frameIndex = frameProxy.getFrameIndex();
            } catch (EvaluateException evaluateException) {
            }
        }


        boolean isInitial = !fromTree.frameIdEquals(frameCount, frameIndex);
        DescriptorTree descriptorTree = new DescriptorTree(isInitial);
        descriptorTree.setFrameId(frameCount, frameIndex);
        return descriptorTree;
    }

    public void dispose() {
        this.myCurrentHistoryTree.clear();
        this.myDescriptorSearcher.clear();
        this.myDisplayDescriptorSearcher.clear();
    }

    @NotNull
    public <T extends NodeDescriptor> T getDescriptor(NodeDescriptor parent, DescriptorData<T> key) {
        NodeDescriptor nodeDescriptor = key.createDescriptor(this.myProject);

        T oldDescriptor = findDescriptor(parent, (T) nodeDescriptor, key);

        if (oldDescriptor != null && oldDescriptor.getClass() == nodeDescriptor.getClass()) {
            nodeDescriptor.setAncestor(oldDescriptor);
        } else {
            T displayDescriptor = findDisplayDescriptor(parent, (T) nodeDescriptor, key.getDisplayKey());
            if (displayDescriptor != null) {
                nodeDescriptor.displayAs(displayDescriptor);
            }
        }

        this.myCurrentHistoryTree.addChild(parent, nodeDescriptor);

        return (T) nodeDescriptor;
    }

    @Nullable
    protected <T extends NodeDescriptor> T findDisplayDescriptor(NodeDescriptor parent, T descriptor, DisplayKey<T> key) {
        return this.myDisplayDescriptorSearcher.search(parent, descriptor, key);
    }

    @Nullable
    protected <T extends NodeDescriptor> T findDescriptor(NodeDescriptor parent, T descriptor, DescriptorData<T> key) {
        return this.myDescriptorSearcher.search(parent, descriptor, key);
    }

    public DescriptorTree getCurrentHistoryTree() {
        return this.myCurrentHistoryTree;
    }

    public void deriveHistoryTree(DescriptorTree tree, StackFrameContext context) {
        deriveHistoryTree(tree, context.getFrameProxy());
    }

    public void deriveHistoryTree(DescriptorTree tree, StackFrameProxy frameProxy) {
        final MarkedDescriptorTree descriptorTree = new MarkedDescriptorTree();
        final MarkedDescriptorTree displayDescriptorTree = new MarkedDescriptorTree();

        tree.dfst(new DescriptorTree.DFSTWalker() {

            public void visit(NodeDescriptor parent, NodeDescriptor child) {
                DescriptorData<NodeDescriptor> descriptorData = DescriptorData.getDescriptorData(child);
                descriptorTree.addChild(parent, child, descriptorData);
                displayDescriptorTree.addChild(parent, child, descriptorData
                        .getDisplayKey());
            }
        });

        this.myDescriptorSearcher = new DescriptorTreeSearcher(descriptorTree);

        this.myDisplayDescriptorSearcher = new DisplayDescriptorTreeSearcher(displayDescriptorTree);


        this.myCurrentHistoryTree = createDescriptorTree(frameProxy, tree);
    }

    public InsidiousArrayElementDescriptorImpl getArrayItemDescriptor(NodeDescriptor parent, ArrayReference array, int index) {
        return new InsidiousArrayElementDescriptorImpl(this.myProject, array, index);
    }


    @NotNull
    public InsidiousFieldDescriptorImpl getFieldDescriptor(NodeDescriptor parent, ObjectReference objRef, Field field) {
        return new InsidiousFieldDescriptorImpl(this.myProject, objRef, field);
    }


    public InsidiousLocalVariableDescriptorImpl getLocalVariableDescriptor(NodeDescriptor parent, InsidiousLocalVariableProxy local) {
        return getDescriptor(parent, new InsidiousLocalData(local));
    }


    public InsidiousArgumentValueDescriptorImpl getArgumentValueDescriptor(NodeDescriptor parent, DecompiledLocalVariable variable, Value value) {
        return getDescriptor(parent, new InsidiousArgValueData(variable, value));
    }


    public InsidiousStackFrameDescriptorImpl getStackFrameDescriptor(@Nullable InsidiousNodeDescriptorImpl parent, @NotNull InsidiousStackFrameProxy frameProxy) {
        return getDescriptor(parent, new InsidiousStackFrameData(frameProxy));
    }


    public InsidiousStaticDescriptorImpl getStaticDescriptor(InsidiousNodeDescriptorImpl parent, ReferenceType refType) {
        return getDescriptor(parent, new InsidiousStaticData(refType));
    }

    public InsidiousValueDescriptorImpl getThisDescriptor(InsidiousNodeDescriptorImpl parent, Value value) {
        return getDescriptor(parent, new InsidiousThisData((InsidiousObjectReference)value));
    }


    public InsidiousValueDescriptorImpl getMethodReturnValueDescriptor(InsidiousNodeDescriptorImpl parent, Method method, Value value) {
        return getDescriptor(parent, new InsidiousMethodReturnValueData(method, value));
    }


    public InsidiousValueDescriptorImpl getThrownExceptionObjectDescriptor(InsidiousNodeDescriptorImpl parent, ObjectReference exceptionObject) {
        return getDescriptor(parent, new InsidiousThrownExceptionValueData(exceptionObject));
    }


    public InsidiousThreadDescriptorImpl getThreadDescriptor(InsidiousNodeDescriptorImpl parent, InsidiousThreadReferenceProxy thread) {
        return getDescriptor(parent, new InsidiousThreadData(thread));
    }


    public InsidiousThreadGroupDescriptorImpl getThreadGroupDescriptor(InsidiousNodeDescriptorImpl parent, InsidiousThreadGroupReferenceProxy group) {
        return getDescriptor(parent, new InsidiousThreadGroupData(group));
    }


    public InsidiousUserExpressionDescriptor getUserExpressionDescriptor(NodeDescriptor parent, DescriptorData<InsidiousUserExpressionDescriptor> data) {
        return getDescriptor(parent, data);
    }


    public InsidiousWatchItemDescriptor getWatchItemDescriptor(NodeDescriptor parent, TextWithImports text, @Nullable Value value) {
        return getDescriptor(parent, new InsidiousWatchItemData(text, value));
    }

    private static class DescriptorTreeSearcher {
        private final MarkedDescriptorTree myDescriptorTree;
        private final HashMap<NodeDescriptor, NodeDescriptor> mySearchedDescriptors = new HashMap<>();


        DescriptorTreeSearcher(MarkedDescriptorTree descriptorTree) {
            this.myDescriptorTree = descriptorTree;
        }


        @Nullable
        public <T extends NodeDescriptor> T search(NodeDescriptor parent, T descriptor, DescriptorKey<T> key) {
            NodeDescriptor nodeDescriptor;
            if (parent == null) {
                nodeDescriptor = this.myDescriptorTree.getChild(null, key);
            } else {
                NodeDescriptor parentDescriptor = getSearched(parent);


                nodeDescriptor = (parentDescriptor != null) ? this.myDescriptorTree.getChild(parentDescriptor, key) : null;
            }
            if (nodeDescriptor != null) {
                this.mySearchedDescriptors.put(descriptor, nodeDescriptor);
            }
            return (T) nodeDescriptor;
        }

        protected NodeDescriptor getSearched(NodeDescriptor parent) {
            return this.mySearchedDescriptors.get(parent);
        }

        public void clear() {
            this.mySearchedDescriptors.clear();
            this.myDescriptorTree.clear();
        }
    }

    private class DisplayDescriptorTreeSearcher
            extends DescriptorTreeSearcher {
        DisplayDescriptorTreeSearcher(MarkedDescriptorTree descriptorTree) {
            super(descriptorTree);
        }


        protected NodeDescriptor getSearched(NodeDescriptor parent) {
            NodeDescriptor searched = super.getSearched(parent);
            if (searched == null) {
                return InsidiousNodeDescriptorFactoryImpl.this.myDescriptorSearcher.getSearched(parent);
            }
            return searched;
        }
    }
}

