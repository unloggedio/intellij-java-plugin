package com.insidious.plugin.extension.descriptor.renderer;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.ui.tree.NodeDescriptor;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.intellij.debugger.ui.tree.render.ReferenceRenderer;
import com.intellij.debugger.ui.tree.render.Renderer;
import org.slf4j.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.xdebugger.frame.XFullValueEvaluator;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.insidious.plugin.extension.InsidiousChildRenderer;
import com.insidious.plugin.extension.InsidiousDebuggerTreeNode;
import com.insidious.plugin.extension.descriptor.InsidiousFullValueEvaluatorProvider;
import com.insidious.plugin.extension.descriptor.InsidiousValueDescriptor;
import com.insidious.plugin.extension.descriptor.InsidiousValueDescriptorImpl;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.concurrent.CompletableFuture;

public class InsidiousCompoundReferenceRenderer
        extends InsidiousNodeRendererImpl
        implements InsidiousFullValueEvaluatorProvider {
    @NonNls
    public static final String UNIQUE_ID = "CompoundNodeRenderer";
    @NonNls
    public static final String UNIQUE_ID_OLD = "CompoundTypeRenderer";
    protected static final Logger LOG = LoggerUtil.getInstance(InsidiousCompoundReferenceRenderer.class);
    private static final AutoToStringRenderer AUTO_TO_STRING_RENDERER = new AutoToStringRenderer();
    private InsidiousValueIconRenderer myIconRenderer = null;
    private InsidiousValueLabelRenderer myLabelRenderer;
    private InsidiousChildRenderer myChildrenRenderer;
    private InsidiousFullValueEvaluatorProvider myFullValueEvaluatorProvider;

    public InsidiousCompoundReferenceRenderer(String name, InsidiousValueLabelRenderer labelRenderer, InsidiousChildRenderer childrenRenderer) {
        super(name);
        this.myLabelRenderer = labelRenderer;
        this.myChildrenRenderer = childrenRenderer;
        this.myProperties.setClassName("java.lang.Object");
        LOG.info("assert label renderer - {}", (labelRenderer == null || labelRenderer instanceof InsidiousReferenceRenderer));
        LOG.info("assert child renderer - {}", (childrenRenderer == null || childrenRenderer instanceof InsidiousReferenceRenderer));
    }


    public void buildChildren(Value value, InsidiousChildrenBuilder builder, EvaluationContext evaluationContext) {
        getChildrenRenderer().buildChildren(value, builder, evaluationContext);
    }


    public PsiElement getChildValueExpression(InsidiousDebuggerTreeNode node, EvaluationContext context) throws EvaluateException {
        return getChildrenRenderer().getChildValueExpression(node, context);
    }


    public CompletableFuture<Boolean> isExpandableAsync(Value value, EvaluationContext evaluationContext, NodeDescriptor parentDescriptor) {
        return getChildrenRenderer().isExpandableAsync(value, evaluationContext, parentDescriptor);
    }


    public String calcLabel(InsidiousValueDescriptor descriptor, EvaluationContext evaluationContext, DescriptorLabelListener listener) throws EvaluateException {
        return getLabelRenderer().calcLabel(descriptor, evaluationContext, listener);
    }


    @Nullable
    public Icon calcValueIcon(InsidiousValueDescriptor descriptor, EvaluationContext evaluationContext, DescriptorLabelListener listener) throws EvaluateException {
        if (this.myIconRenderer != null) {
            return this.myIconRenderer.calcValueIcon(descriptor, evaluationContext, listener);
        }
        return null;
    }

    void setIconRenderer(InsidiousValueIconRenderer iconRenderer) {
        this.myIconRenderer = iconRenderer;
    }


    @Nullable
    public XFullValueEvaluator getFullValueEvaluator(EvaluationContext evaluationContext, InsidiousValueDescriptorImpl valueDescriptor) {
        if (this.myFullValueEvaluatorProvider != null) {
            return this.myFullValueEvaluatorProvider.getFullValueEvaluator(evaluationContext, valueDescriptor);
        }

        return null;
    }

    void setFullValueEvaluator(InsidiousFullValueEvaluatorProvider fullValueEvaluatorProvider) {
        this.myFullValueEvaluatorProvider = fullValueEvaluatorProvider;
    }

    public InsidiousChildRenderer getChildrenRenderer() {
        return (this.myChildrenRenderer != null) ? this.myChildrenRenderer : getDefaultRenderer();
    }

    public void setChildrenRenderer(InsidiousChildRenderer childrenRenderer) {
        InsidiousChildRenderer prevRenderer = getChildrenRenderer();
        this.myChildrenRenderer = isBaseRenderer(childrenRenderer) ? null : childrenRenderer;
        InsidiousChildRenderer currentRenderer = getChildrenRenderer();
        if (prevRenderer != currentRenderer &&
                currentRenderer instanceof ReferenceRenderer) {
            ((ReferenceRenderer) currentRenderer).setClassName(getClassName());
        }
    }

    private InsidiousNodeRenderer getDefaultRenderer() {
        String name = getClassName();
        if (TypeConversionUtil.isPrimitive(name)) {
            return RendererManager.getInstance().getPrimitiveRenderer();
        }
        return name.endsWith("]") ?
                RendererManager.getInstance().getArrayRenderer() :
                AUTO_TO_STRING_RENDERER;
    }

    public InsidiousValueLabelRenderer getLabelRenderer() {
        return (this.myLabelRenderer != null) ? this.myLabelRenderer : getDefaultRenderer();
    }

    public void setLabelRenderer(InsidiousValueLabelRenderer labelRenderer) {
        InsidiousValueLabelRenderer prevRenderer = getLabelRenderer();
        this.myLabelRenderer = isBaseRenderer(labelRenderer) ? null : labelRenderer;
        InsidiousValueLabelRenderer currentRenderer = getLabelRenderer();
        if (prevRenderer != currentRenderer &&
                currentRenderer instanceof ReferenceRenderer) {
            ((ReferenceRenderer) currentRenderer).setClassName(getClassName());
        }
    }

    private InsidiousChildRenderer getRawChildrenRenderer() {
        return (this.myChildrenRenderer == getDefaultRenderer()) ? null : this.myChildrenRenderer;
    }

    private InsidiousValueLabelRenderer getRawLabelRenderer() {
        return (this.myLabelRenderer == getDefaultRenderer()) ? null : this.myLabelRenderer;
    }

    public boolean isApplicable(Type type) {
        String className = getClassName();
        if (!StringUtil.isEmpty(className)) {
            return DebuggerUtils.instanceOf(type, className);
        }
        return (getLabelRenderer().isApplicable(type) && getChildrenRenderer().isApplicable(type));
    }

    @NotNull
    public String getClassName() {
        return this.myProperties.getClassName();
    }

    public void setClassName(@NotNull String name) {
        this.myProperties.setClassName(name);
        if (getRawLabelRenderer() != null &&
                this.myLabelRenderer instanceof ReferenceRenderer) {
            ((ReferenceRenderer) this.myLabelRenderer).setClassName(name);
        }


        if (getRawChildrenRenderer() != null &&
                this.myChildrenRenderer instanceof ReferenceRenderer) {
            ((ReferenceRenderer) this.myChildrenRenderer).setClassName(name);
        }
    }

    protected final PsiElement getContext(Project project, EvaluationContext context) {
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        return DebuggerUtils.findClass(getClassName(), project, scope);
    }


    protected final PsiElement getChildValueExpression(String text, InsidiousDebuggerTreeNode node, EvaluationContext context) {
        Project project = node.getProject();
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
        return elementFactory.createExpressionFromText(text, getContext(project, context));
    }

    public boolean isBaseRenderer(Renderer renderer) {
        return (renderer == AUTO_TO_STRING_RENDERER || renderer ==
                RendererManager.getInstance().getClassRenderer() || renderer ==
                RendererManager.getInstance().getPrimitiveRenderer() || renderer ==
                RendererManager.getInstance().getArrayRenderer());
    }

    public String getUniqueId() {
        return "CompoundNodeRenderer";
    }

    public InsidiousCompoundReferenceRenderer clone() {
        InsidiousCompoundReferenceRenderer renderer = (InsidiousCompoundReferenceRenderer) super.clone();
        renderer
                .myLabelRenderer = (this.myLabelRenderer != null) ? (InsidiousValueLabelRenderer) this.myLabelRenderer.clone() : null;
        renderer


                .myChildrenRenderer = (this.myChildrenRenderer != null) ? (InsidiousChildRenderer) this.myChildrenRenderer.clone() : null;
        return renderer;
    }

    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);
    }

    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);
    }

    private static class AutoToStringRenderer extends InsidiousToStringRenderer {
        private AutoToStringRenderer() {
            setIsApplicableChecker(type -> CompletableFuture.completedFuture(Boolean.valueOf(type instanceof com.sun.jdi.ReferenceType)));
        }


        public String getUniqueId() {
            return "AutoToString";
        }


        public boolean isOnDemand(EvaluationContext evaluationContext, InsidiousValueDescriptor valueDescriptor) {
            return RendererManager.getInstance()
                    .getToStringRenderer()
                    .isOnDemand(evaluationContext, valueDescriptor);
        }


        public String calcLabel(InsidiousValueDescriptor descriptor, EvaluationContext evaluationContext, DescriptorLabelListener listener) throws EvaluateException {
            InsidiousToStringRenderer toStringRenderer = RendererManager.getInstance().getToStringRenderer();
            if (toStringRenderer.isEnabled() && toStringRenderer
                    .isApplicable(descriptor.getType())) {
                return toStringRenderer.calcLabel(descriptor, evaluationContext, listener);
            }
            return RendererManager.getInstance()
                    .getClassRenderer()
                    .calcLabel(descriptor, evaluationContext, listener);
        }
    }
}

