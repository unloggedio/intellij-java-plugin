package com.insidious.plugin.extension.thread;

import com.insidious.plugin.extension.InsidiousJavaDebugProcess;
import com.insidious.plugin.extension.InsidiousXSuspendContext;
import com.insidious.plugin.extension.connector.InsidiousStackFrameProxy;
import com.insidious.plugin.extension.descriptor.InsidiousJavaValue;
import com.insidious.plugin.extension.descriptor.InsidiousStackFrameDescriptorImpl;
import com.insidious.plugin.extension.descriptor.renderer.InsidiousNodeManagerImpl;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import com.insidious.plugin.extension.evaluation.EvaluationContextImpl;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.ui.impl.watch.MethodsTracker;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XNamedValue;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XValueChildrenList;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StackFrame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class InsidiousXStackFrame extends XStackFrame {

    private static final Logger logger = LoggerUtil.getInstance(XStackFrame.class);

    private final InsidiousJavaDebugProcess insidiousJavaDebugProcess;
    private final InsidiousXSuspendContext insidiousXSuspendContext;
    private final InsidiousStackFrameProxy insidiousStackFrameProxy;
    private final XSourcePosition xSourcePosition;
    private final InsidiousStackFrameDescriptorImpl myDescriptor;

    public InsidiousXStackFrame(InsidiousJavaDebugProcess insidiousJavaDebugProcess,
                                InsidiousXSuspendContext insidiousXSuspendContext,
                                InsidiousStackFrameProxy insidiousStackFrameProxy,
                                XSourcePosition xSourcePosition) {
        this.insidiousJavaDebugProcess = insidiousJavaDebugProcess;
        this.insidiousXSuspendContext = insidiousXSuspendContext;
        this.insidiousStackFrameProxy = insidiousStackFrameProxy;
        this.xSourcePosition = xSourcePosition;
        this.myDescriptor = new InsidiousStackFrameDescriptorImpl(insidiousStackFrameProxy, new MethodsTracker());
        this.myDescriptor.updateRepresentation(null, DescriptorLabelListener.DUMMY_LISTENER);
    }

    @Override
    public @Nullable XSourcePosition getSourcePosition() {
        return xSourcePosition;
    }


    @Override
    public void computeChildren(@NotNull XCompositeNode node) {
        super.computeChildren(node);
        @NotNull XValueChildrenList children = new XValueChildrenList();
        ObjectReference thisObject = null;
        try {
            thisObject = insidiousStackFrameProxy.thisObject();
        } catch (EvaluateException e) {
            node.setErrorMessage(e.getMessage());
        }

        EvaluationContextImpl evaluationContextImpl = new EvaluationContextImpl(this.insidiousXSuspendContext, this.insidiousStackFrameProxy, thisObject);

        buildVariables(evaluationContextImpl, children);
        node.addChildren(children, true);
    }


    protected XNamedValue createThisNode(EvaluationContext context) {
        InsidiousNodeManagerImpl nodeManager = this.insidiousJavaDebugProcess.getInsidiousNodeManager();
        ObjectReference thisObjectReference = this.myDescriptor.getThisObject();
        if (thisObjectReference != null) {
            return InsidiousJavaValue.create(nodeManager.getThisDescriptor(null, thisObjectReference), context, nodeManager);
        }
        return null;
    }

    private void buildVariables(EvaluationContext evaluationContextImpl, XValueChildrenList children) {

        StackFrame stackFrame;
        try {

            XNamedValue thisNode = createThisNode(evaluationContextImpl);
            if (thisNode != null) {
                children.add(thisNode);
            }


            stackFrame = this.insidiousStackFrameProxy.getStackFrame();
            List<LocalVariable> variables = stackFrame.visibleVariables();

            for (LocalVariable variable : variables) {
                children.add(variable.name(), new InsidiousXValue(((InsidiousLocalVariable) variable).getValue()));
            }

        } catch (EvaluateException | AbsentInformationException e) {
            e.printStackTrace();
            return;
        }

    }


}
