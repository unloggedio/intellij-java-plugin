package com.insidious.plugin.extension.descriptor;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.expression.Modifier;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.ui.tree.NodeDescriptor;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.intellij.debugger.ui.tree.render.Renderer;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import org.slf4j.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ThreeState;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.evaluation.XInstanceEvaluator;
import com.intellij.xdebugger.frame.*;
import com.intellij.xdebugger.frame.presentation.XRegularValuePresentation;
import com.intellij.xdebugger.frame.presentation.XValuePresentation;
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl;
import com.insidious.plugin.extension.DebuggerBundle;
import com.insidious.plugin.extension.InsidiousDebuggerTreeNode;
import com.insidious.plugin.extension.descriptor.renderer.*;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import com.insidious.plugin.extension.evaluation.EvaluationValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import javax.swing.*;
import java.util.List;
import java.util.Objects;

public class InsidiousJavaValue extends XNamedValue implements InsidiousNodeDescriptorProvider {
    private static final Logger LOG = LoggerUtil.getInstance(InsidiousJavaValue.class);

    private final boolean myCanBePinned;

    private final InsidiousJavaValue myParent;
    @NotNull
    private final InsidiousValueDescriptorImpl myValueDescriptor;
    @NotNull
    private final EvaluationContext myEvaluationContext;
    private final InsidiousNodeManagerImpl myNodeManager;
    private final boolean myContextSet;
    private final XExpression evaluationExpression;
    private int myChildrenRemaining;

    protected InsidiousJavaValue(InsidiousJavaValue parent, @NotNull InsidiousValueDescriptorImpl valueDescriptor, @NotNull EvaluationContext evaluationContext, InsidiousNodeManagerImpl nodeManager, boolean contextSet) {
        this(parent, valueDescriptor

                .calcValueName(), valueDescriptor, evaluationContext, nodeManager, contextSet);
    }


    protected InsidiousJavaValue(InsidiousJavaValue parent, String name, @NotNull InsidiousValueDescriptorImpl valueDescriptor, @NotNull EvaluationContext evaluationContext, InsidiousNodeManagerImpl nodeManager, boolean contextSet) {
        super(name);


        this.myChildrenRemaining = -1;


        this.evaluationExpression = null;
        this.myParent = parent;
        this.myValueDescriptor = valueDescriptor;
        this.myEvaluationContext = evaluationContext;
        this.myNodeManager = nodeManager;
        this.myContextSet = contextSet;
        this.myCanBePinned = doComputeCanBePinned();
    }

    public static InsidiousJavaValue create(InsidiousJavaValue parent, @NotNull InsidiousValueDescriptorImpl valueDescriptor, @NotNull EvaluationContext evaluationContext, InsidiousNodeManagerImpl nodeManager, boolean contextSet) {
        return new InsidiousJavaValue(parent, valueDescriptor, evaluationContext, nodeManager, contextSet);
    }

    public static InsidiousJavaValue create(@NotNull InsidiousValueDescriptorImpl valueDescriptor, @NotNull EvaluationContext evaluationContext, InsidiousNodeManagerImpl nodeManager) {
        return create(null, valueDescriptor, evaluationContext, nodeManager, false);
    }

    public static XValuePresentation createPresentation(InsidiousValueDescriptorImpl descriptor) {
        Renderer lastLabelRenderer = descriptor.getLastLabelRenderer();
        return new InsidiousJavaValuePresentation(descriptor);
    }

    private boolean doComputeCanBePinned() {
        if (this.myValueDescriptor instanceof com.intellij.debugger.ui.tree.ArrayElementDescriptor) return false;
        return (this.myParent != null);
    }

    public InsidiousJavaValue getParent() {
        return this.myParent;
    }

    public InsidiousValueDescriptorImpl getDescriptor() {
        return this.myValueDescriptor;
    }

    @NotNull
    public EvaluationContext getEvaluationContext() {
        return this.myEvaluationContext;
    }

    public InsidiousNodeManagerImpl getNodeManager() {
        return this.myNodeManager;
    }

    private boolean isOnDemand() {
        return InsidiousOnDemandRenderer.ON_DEMAND_CALCULATED.isIn(this.myValueDescriptor);
    }

    private boolean isCalculated() {
        return InsidiousOnDemandRenderer.isCalculated(this.myValueDescriptor);
    }

    public void computePresentation(@NotNull final XValueNode node, @NotNull final XValuePlace place) {
        if (isOnDemand() && !isCalculated()) {
            node.setFullValueEvaluator(InsidiousOnDemandRenderer.createFullValueEvaluator(DebuggerBundle.message("message.node.evaluate")));
            node.setPresentation(AllIcons.Debugger.Db_watch, new XRegularValuePresentation("", null, ""), false);
            return;
        }
        if (node.isObsolete()) return;
        if (!this.myContextSet) {
            ApplicationManager.getApplication().runWriteAction(() -> this.myValueDescriptor.setContext(this.myEvaluationContext));
        }
        XDebugProcess xDebugProcess = this.myEvaluationContext.getVirtualMachineProxy().getXDebugProcess();
        this.myValueDescriptor.updateRepresentationNoNotify(xDebugProcess,
                new DescriptorLabelListener() {
                    public void labelChanged() {
                        Icon nodeIcon = (place == XValuePlace.TOOLTIP) ? InsidiousJavaValue.this.myValueDescriptor.getValueIcon() : InsidiousDebuggerTreeRenderer.getValueIcon(InsidiousJavaValue.this.myValueDescriptor, (InsidiousJavaValue.this.myParent != null) ? InsidiousJavaValue.this.myParent.getDescriptor() : null);
                        XValuePresentation presentation = InsidiousJavaValue.createPresentation(InsidiousJavaValue.this.myValueDescriptor);
                        Renderer lastRenderer = InsidiousJavaValue.this.myValueDescriptor.getLastRenderer();
                        boolean fullEvaluatorSet = setFullValueEvaluator(lastRenderer);
                        if (!fullEvaluatorSet && lastRenderer instanceof InsidiousCompoundReferenceRenderer)
                            fullEvaluatorSet = setFullValueEvaluator(((InsidiousCompoundReferenceRenderer) lastRenderer).getLabelRenderer());
                        if (!fullEvaluatorSet) {
                            final String text = InsidiousJavaValue.this.myValueDescriptor.getValueText();
                            if (text.length() > 1000) {
                                node.setFullValueEvaluator(
                                        new JavaFullValueEvaluator(InsidiousJavaValue.this.myEvaluationContext) {
                                            public void evaluate(@NotNull final XFullValueEvaluator.XFullValueEvaluationCallback callback) {
                                                final InsidiousValueDescriptorImpl fullValueDescriptor = InsidiousJavaValue.this.myValueDescriptor.getFullValueDescriptor();
                                                fullValueDescriptor.updateRepresentation(
                                                        this.myEvaluationContext.getVirtualMachineProxy().getXDebugProcess(),
                                                        () -> callback.evaluated(fullValueDescriptor.getValueText())
                                                );
                                            }
                                        }
                                );
                            } else if (StringUtil.containsLineBreak(text)) {
                                node.setFullValueEvaluator(
                                        new XFullValueEvaluator() {
                                            public void startEvaluation(
                                                    @NotNull XFullValueEvaluator.XFullValueEvaluationCallback callback
                                            ) {
                                                callback.evaluated(text);
                                            }
                                        }
                                );
                            }
                        }
                        node.setPresentation(nodeIcon, presentation, InsidiousJavaValue.this.myValueDescriptor.isExpandable());
                    }

                    private boolean setFullValueEvaluator(Renderer renderer) {
                        return false;
                    }
                }
        );
    }

    @NotNull
    public Promise<XExpression> calculateEvaluationExpression() {
        if (this.evaluationExpression != null) {
            return Promises.resolvedPromise(this.evaluationExpression);
        }
        AsyncPromise<XExpression> result = new AsyncPromise();
        try {
            getDescriptor()
                    .getTreeEvaluation(this)
                    .whenComplete((psiExpression, ex) -> {
                        if (ex != null) {
                            result.setError(ex);
                        } else if (psiExpression != null) {
                            new Exception().printStackTrace();
//                            ReadAction.run(());
                        } else {
                            result.setError("Null");
                        }
                    });
        } catch (EvaluateException e) {
            LOG.info("failed", e);
            result.setError(e);
        }
        return result;
    }

    public void computeChildren(@NotNull final XCompositeNode node) {
        this.myValueDescriptor.getChildrenRenderer().thenAccept(r -> r.buildChildren(this.myValueDescriptor.getValue(),
                new InsidiousChildrenBuilder() {
                    public InsidiousNodeDescriptorFactory getDescriptorManager() {
                        return InsidiousJavaValue.this.myNodeManager;
                    }

                    public InsidiousNodeManager getNodeManager() {
                        return InsidiousJavaValue.this.myNodeManager;
                    }

                    public InsidiousValueDescriptor getParentDescriptor() {
                        return InsidiousJavaValue.this.myValueDescriptor;
                    }

                    public void initChildrenArrayRenderer(InsidiousArrayRenderer renderer, int arrayLength) {
                        renderer.START_INDEX = 0;
                        if (InsidiousJavaValue.this.myChildrenRemaining >= 0)
                            renderer.START_INDEX = Math.max(0, arrayLength - InsidiousJavaValue.this.myChildrenRemaining);
                    }

                    public void addChildren(List<? extends InsidiousDebuggerTreeNode> nodes, boolean last) {
                        XValueChildrenList childrenList = XValueChildrenList.EMPTY;
                        if (!nodes.isEmpty()) {
                            childrenList = new XValueChildrenList(nodes.size());
                            for (InsidiousDebuggerTreeNode treeNode : nodes) {
                                NodeDescriptor descriptor = treeNode.getDescriptor();
                                if (descriptor instanceof InsidiousValueDescriptorImpl) {
                                    childrenList.add(
                                            InsidiousJavaValue.create(
                                                    InsidiousJavaValue.this,
                                                    (InsidiousValueDescriptorImpl) descriptor,
                                                    InsidiousJavaValue.this.myEvaluationContext,
                                                    InsidiousJavaValue.this.myNodeManager,
                                                    false));
                                    continue;
                                }
                                if (descriptor instanceof InsidiousMessageDescriptor)
                                    childrenList.add(new EvaluationValue(descriptor.getLabel(), "", ""));
                            }
                        }
                        node.addChildren(childrenList, last);
                    }

                    public void setChildren(List<? extends InsidiousDebuggerTreeNode> nodes) {
                        addChildren(nodes, true);
                    }

                    public void setMessage(@NotNull String message, @Nullable Icon icon, @NotNull SimpleTextAttributes attributes, @Nullable XDebuggerTreeNodeHyperlink link) {
                        node.setMessage(message, icon, attributes, link);
                    }

                    public void addChildren(@NotNull XValueChildrenList children, boolean last) {
                        node.addChildren(children, last);
                    }

                    public void tooManyChildren(int remaining) {
                        InsidiousJavaValue.this.myChildrenRemaining = remaining;
                        node.tooManyChildren(remaining);
                    }

                    public void setAlreadySorted(boolean alreadySorted) {
                        node.setAlreadySorted(alreadySorted);
                    }

                    public void setErrorMessage(@NotNull String errorMessage) {
                        node.setErrorMessage(errorMessage);
                    }

                    public void setErrorMessage(@NotNull String errorMessage, @Nullable XDebuggerTreeNodeHyperlink link) {
                        node.setErrorMessage(errorMessage, link);
                    }

                    public boolean isObsolete() {
                        return node.isObsolete();
                    }
                }, this.myEvaluationContext));
    }

    public void computeSourcePosition(@NotNull XNavigatable navigatable) {
        computeSourcePosition(navigatable, false);
    }

    private void computeSourcePosition(@NotNull XNavigatable navigatable, boolean inline) {
        ApplicationManager.getApplication().runReadAction(() -> {
            XSourcePosition xPosition = this.myEvaluationContext.getXSuspendContext().getActiveExecutionStack().getTopFrame().getSourcePosition();
            XDebugProcess debugProcess = this.myEvaluationContext.getVirtualMachineProxy().getXDebugProcess();
            SourcePosition position = DebuggerUtilsEx.toSourcePosition(xPosition, debugProcess.getSession().getProject());
            if (position != null) navigatable.setSourcePosition(DebuggerUtilsEx.toXSourcePosition(position));
        });
    }

    @NotNull
    public ThreeState computeInlineDebuggerData(@NotNull XInlineDebuggerDataCallback callback) {
        Objects.requireNonNull(callback);
        computeSourcePosition(callback::computed, true);
        return ThreeState.YES;
    }

    public Project getProject() {
        return this.myValueDescriptor.getProject();
    }

    public boolean canNavigateToTypeSource() {
        return true;
    }

    public void computeTypeSourcePosition(@NotNull XNavigatable navigatable) {
        if (this.myEvaluationContext.getXSuspendContext() == null) return;
        try {
            XSourcePosition position = this.myEvaluationContext.getXSuspendContext().getActiveExecutionStack().getTopFrame().getSourcePosition();
            ApplicationManager.getApplication().runReadAction(() -> navigatable.setSourcePosition(position));
        } catch (Exception exception) {
        }
    }

    @Nullable
    public XValueModifier getModifier() {
        return this.myValueDescriptor.canSetValue() ? this.myValueDescriptor.getModifier(this) : null;
    }

    @Nullable
    public String getValueText() {
        if (this.myValueDescriptor.getLastLabelRenderer() instanceof com.intellij.debugger.ui.tree.render.XValuePresentationProvider) {
            return null;
        }
        return this.myValueDescriptor.getValueText();
    }

    @Nullable
    public XReferrersProvider getReferrersProvider() {
        return new XReferrersProvider() {
            public XValue getReferringObjectsValue() {
                InsidiousReferringObjectsProvider provider = InsidiousReferringObjectsProvider.BASIC_JDI;
                return new InsidiousJavaReferringObjectsValue(InsidiousJavaValue.this, provider, null);
            }
        };
    }

    @Nullable
    public XInstanceEvaluator getInstanceEvaluator() {
        return new XInstanceEvaluator() {

            public void evaluate(@NotNull XDebuggerEvaluator.XEvaluationCallback callback, @NotNull XStackFrame frame) {
                InsidiousValueDescriptorImpl inspectDescriptor = InsidiousJavaValue.this.myValueDescriptor;
                if (InsidiousJavaValue.this.myValueDescriptor instanceof InsidiousWatchItemDescriptor) {
                    Modifier modifier = ((InsidiousWatchItemDescriptor) InsidiousJavaValue.this.myValueDescriptor).getModifier();
                    if (modifier != null) {
                        NodeDescriptor item = modifier.getInspectItem(InsidiousJavaValue.this.getProject());
                        if (item != null) {
                            inspectDescriptor = (InsidiousValueDescriptorImpl) item;
                        }
                    }
                }

                if (InsidiousJavaValue.this.myEvaluationContext != null) {
                    callback.evaluated(
                            InsidiousJavaValue.create(inspectDescriptor, InsidiousJavaValue.this.myEvaluationContext, InsidiousJavaValue.this.myNodeManager));
                } else {
                    callback.errorOccurred("context not found");
                }
            }
        };
    }

    public void setRenderer(InsidiousNodeRenderer nodeRenderer, XValueNodeImpl node) {
        this.myValueDescriptor.setRenderer(nodeRenderer);
        reBuild(node);
    }

    public void reBuild(XValueNodeImpl node) {
        this.myChildrenRemaining = -1;
        node.invokeNodeUpdate(() -> {
            node.clearChildren();
            computePresentation(node, XValuePlace.TREE);
        });
    }

    @Nullable
    public String computeInlinePresentation() {
        InsidiousValueDescriptorImpl descriptor = getDescriptor();
        return (descriptor.isNull() || descriptor.isPrimitive()) ? descriptor.getValueText() : null;
    }

    public static abstract class JavaFullValueEvaluator extends XFullValueEvaluator {
        protected final EvaluationContext myEvaluationContext;

        public JavaFullValueEvaluator(@NotNull String linkText, EvaluationContext evaluationContext) {
            super(linkText);
            this.myEvaluationContext = evaluationContext;
        }

        public JavaFullValueEvaluator(EvaluationContext evaluationContext) {
            this.myEvaluationContext = evaluationContext;
        }

        public abstract void evaluate(@NotNull XFullValueEvaluator.XFullValueEvaluationCallback param1XFullValueEvaluationCallback) throws Exception;

        protected EvaluationContext getEvaluationContext() {
            return this.myEvaluationContext;
        }

        public void startEvaluation(@NotNull XFullValueEvaluator.XFullValueEvaluationCallback callback) {
            if (callback.isObsolete()) return;
            try {
                evaluate(callback);
            } catch (Exception exception) {
            }
        }
    }
}

