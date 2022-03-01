package com.insidious.plugin.extension.descriptor.renderer;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.settings.NodeRendererSettings;
import com.intellij.debugger.ui.overhead.OverheadProducer;
import com.intellij.debugger.ui.tree.NodeDescriptor;
import com.intellij.debugger.ui.tree.render.BasicRendererProperties;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.intellij.psi.PsiElement;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.xdebugger.XDebuggerBundle;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.insidious.plugin.extension.InsidiousDebuggerTreeNode;
import com.insidious.plugin.extension.descriptor.InsidiousValueDescriptor;
import com.insidious.plugin.extension.descriptor.InsidiousValueDescriptorImpl;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import org.jdom.Element;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public abstract class InsidiousNodeRendererImpl
        implements InsidiousNodeRenderer {
    public static final String DEFAULT_NAME = "unnamed";
    private final String myDefaultName;
    protected BasicRendererProperties myProperties;
    private Function<Type, CompletableFuture<Boolean>> myIsApplicableChecker = null;

    protected InsidiousNodeRendererImpl() {
        this("unnamed", false);
    }

    protected InsidiousNodeRendererImpl(@NotNull String presentableName) {
        this(presentableName, false);
    }

    protected InsidiousNodeRendererImpl(@NotNull String presentableName, boolean enabledDefaultValue) {
        this.myDefaultName = presentableName;
        this.myProperties = new BasicRendererProperties(enabledDefaultValue);
        this.myProperties.setName(presentableName);
        this.myProperties.setEnabled(enabledDefaultValue);
    }

    public static String calcLabel(CompletableFuture<InsidiousNodeRenderer> renderer, InsidiousValueDescriptor descriptor, EvaluationContext evaluationContext, DescriptorLabelListener listener) {
        return renderer.thenApply(r -> {

            try {
                return r.calcLabel(descriptor, evaluationContext, listener);
            } catch (EvaluateException e) {
                descriptor.setValueLabelFailed(e);

                listener.labelChanged();
                return "";
            }
        }).getNow(XDebuggerBundle.message("xdebugger.building.tree.node.message"));
    }

    public String getName() {
        return this.myProperties.getName();
    }

    public void setName(String name) {
        this.myProperties.setName(name);
    }

    public boolean isEnabled() {
        return this.myProperties.isEnabled();
    }

    public void setEnabled(boolean enabled) {
        this.myProperties.setEnabled(enabled);
    }

    public boolean isShowType() {
        return this.myProperties.isShowType();
    }

    public void setShowType(boolean showType) {
        this.myProperties.setShowType(showType);
    }

    public void buildChildren(Value value, InsidiousChildrenBuilder builder, EvaluationContext evaluationContext) {
    }

    public PsiElement getChildValueExpression(InsidiousDebuggerTreeNode node, EvaluationContext context) throws EvaluateException {
        return null;
    }

    @Internal
    public void setIsApplicableChecker(@NotNull Function<Type, CompletableFuture<Boolean>> isApplicableAsync) {
        this.myIsApplicableChecker = isApplicableAsync;
    }

    public boolean isExpandable(Value value, EvaluationContext evaluationContext, NodeDescriptor parentDescriptor) {
        return false;
    }

    public InsidiousNodeRendererImpl clone() {
        try {
            InsidiousNodeRendererImpl cloned = (InsidiousNodeRendererImpl) super.clone();
            cloned.myProperties = this.myProperties.clone();
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public void readExternal(Element element) {
        this.myProperties.readExternal(element, this.myDefaultName);
    }

    public void writeExternal(Element element) {
        this.myProperties.writeExternal(element, this.myDefaultName);
    }

    public String toString() {
        return getName();
    }

    @Nullable
    public String calcIdLabel(InsidiousValueDescriptor descriptor, DescriptorLabelListener labelListener) {
        Value value = descriptor.getValue();
        if (!(value instanceof com.sun.jdi.ObjectReference) || !isShowType()) {
            return null;
        }
        return InsidiousValueDescriptorImpl.calcIdLabel(descriptor, labelListener);
    }

    public boolean hasOverhead() {
        return false;
    }

    public static class Overhead implements OverheadProducer {
        private final InsidiousNodeRendererImpl myRenderer;

        public Overhead(@NotNull InsidiousNodeRendererImpl renderer) {
            this.myRenderer = renderer;
        }


        public boolean isEnabled() {
            return this.myRenderer.isEnabled();
        }


        public void setEnabled(boolean enabled) {
            this.myRenderer.setEnabled(enabled);
            NodeRendererSettings.getInstance().fireRenderersChanged();
        }


        public void customizeRenderer(SimpleColoredComponent renderer) {
            renderer.append(this.myRenderer.getName() + " renderer");
        }


        public int hashCode() {
            return this.myRenderer.hashCode();
        }


        public boolean equals(Object obj) {
            return (obj instanceof Overhead && this.myRenderer
                    .equals(((Overhead) obj).myRenderer));
        }
    }
}


