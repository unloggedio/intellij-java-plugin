package extension.descriptor.renderer;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.jdi.StackFrameProxy;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.ui.impl.watch.DescriptorTree;
import com.intellij.debugger.ui.tree.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugProcess;
import com.sun.jdi.*;
import extension.InsidiousDebugProcess;
import extension.InsidiousDebuggerTreeNode;
import extension.InsidiousLocalVariableProxy;
import extension.descriptor.*;
import extension.evaluation.EvaluationContext;
import extension.evaluation.InsidiousNodeDescriptorImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class InsidiousNodeManagerImpl extends InsidiousNodeDescriptorFactoryImpl implements InsidiousNodeManager {
    private static final Comparator<InsidiousDebuggerTreeNode> ourNodeComparator = new InsidiousNodeComparator();

    private final InsidiousDebuggerTree myDebuggerTree;
    private final Map<String, DescriptorTree> myHistories = new HashMap<>();
    private final InsidiousDebugProcess InsidiousJavaDebugProcess;
    private String myHistoryKey = null;


    public InsidiousNodeManagerImpl(Project project, InsidiousDebuggerTree tree, InsidiousDebugProcess InsidiousJavaDebugProcess) {
        super(project);
        this.myDebuggerTree = tree;
        this.InsidiousJavaDebugProcess = InsidiousJavaDebugProcess;
    }

    public static Comparator<InsidiousDebuggerTreeNode> getNodeComparator() {
        return ourNodeComparator;
    }

    @Nullable
    public static String getContextKeyForFrame(StackFrameProxy frame) {
        if (frame == null) {
            return null;
        }

        try {
            Location location = frame.location();
            Method method = DebuggerUtilsEx.getMethod(location);
            if (method == null) {
                return null;
            }
            return location.declaringType().signature() + "#" + method.name() + method.signature();
        } catch (EvaluateException evaluateException) {
        } catch (InternalException ie) {
            if (ie.errorCode() != 23) {
                throw ie;
            }
        }

        return null;
    }

    @NotNull
    public InsidiousDebuggerTreeNodeImpl createNode(NodeDescriptor descriptor, EvaluationContext evaluationContext) {
        ((InsidiousNodeDescriptorImpl) descriptor).setContext(evaluationContext);
        return InsidiousDebuggerTreeNodeImpl.createNode(getTree(), (InsidiousNodeDescriptorImpl) descriptor, this.InsidiousJavaDebugProcess);
    }

    public InsidiousDebuggerTreeNodeImpl getDefaultNode() {
        return InsidiousDebuggerTreeNodeImpl.createNodeNoUpdate(
                getTree(), new InsidiousDefaultNodeDescriptor(), this.InsidiousJavaDebugProcess);
    }

    public InsidiousDebuggerTreeNodeImpl createMessageNode(InsidiousMessageDescriptor descriptor) {
        return InsidiousDebuggerTreeNodeImpl.createNodeNoUpdate(
                getTree(), descriptor, this.InsidiousJavaDebugProcess);
    }

    @NotNull
    public InsidiousDebuggerTreeNodeImpl createMessageNode(String message) {
        return InsidiousDebuggerTreeNodeImpl.createNodeNoUpdate(getTree(), new InsidiousMessageDescriptor(message), this.InsidiousJavaDebugProcess);
    }

    public void setHistoryByContext(XDebugProcess context) {
        setHistoryByContext(context);
    }

    public void setHistoryByContext(StackFrameProxy frameProxy) {
        DescriptorTree descriptorTree;
        if (this.myHistoryKey != null) {
            this.myHistories.put(this.myHistoryKey, getCurrentHistoryTree());
        }

        String historyKey = getContextKey(frameProxy);

        if (historyKey != null) {
            DescriptorTree historyTree = this.myHistories.get(historyKey);
            descriptorTree = (historyTree != null) ? historyTree : new DescriptorTree(true);
        } else {
            descriptorTree = new DescriptorTree(true);
        }

        deriveHistoryTree(descriptorTree, frameProxy);
        this.myHistoryKey = historyKey;
    }

    @Nullable
    public String getContextKey(StackFrameProxy frame) {
        return getContextKeyForFrame(frame);
    }

    public void dispose() {
        this.myHistories.clear();
        super.dispose();
    }

    private InsidiousDebuggerTree getTree() {
        return this.myDebuggerTree;
    }


    public InsidiousLocalVariableDescriptorImpl getLocalVariableDescriptor(NodeDescriptor parent, InsidiousLocalVariableProxy local) {
        InsidiousLocalVariableDescriptorImpl descriptor = super.getLocalVariableDescriptor(parent, local);

        descriptor.myIsSynthetic = true;
        return descriptor;
    }


    @NotNull
    public InsidiousFieldDescriptorImpl getFieldDescriptor(NodeDescriptor parent, ObjectReference objRef, Field field) {
        InsidiousFieldDescriptorImpl descriptor = super.getFieldDescriptor(parent, objRef, field);
        descriptor.myIsSynthetic = true;
        return descriptor;
    }


    public InsidiousArrayElementDescriptorImpl getArrayItemDescriptor(NodeDescriptor parent, ArrayReference array, int index) {
        InsidiousArrayElementDescriptorImpl descriptor = super.getArrayItemDescriptor(parent, array, index);
        descriptor.myIsSynthetic = true;
        return descriptor;
    }
}


