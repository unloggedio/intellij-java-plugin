package com.insidious.plugin.extension.descriptor.renderer;


import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.ui.tree.NodeDescriptor;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.ui.classFilter.ClassFilter;
import com.sun.jdi.*;
import com.insidious.plugin.extension.DebuggerBundle;
import com.insidious.plugin.extension.InsidiousDebuggerTreeNode;
import com.insidious.plugin.extension.descriptor.InsidiousValueDescriptor;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import com.insidious.plugin.extension.evaluation.EvaluatorUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;


public class InsidiousToStringRenderer extends InsidiousNodeRendererImpl implements InsidiousOnDemandRenderer {
    @NonNls
    public static final String UNIQUE_ID = "ToStringRenderer";
    private ClassFilter[] myClassFilters = ClassFilter.EMPTY_ARRAY;
    private boolean USE_CLASS_FILTERS = false;
    private boolean ON_DEMAND;

    public InsidiousToStringRenderer() {
        super("unnamed", true);
        setIsApplicableChecker(type ->


                (!(type instanceof ReferenceType) || "java.lang.String".equals(type.name())) ? CompletableFuture.completedFuture(Boolean.valueOf(false)) : overridesToStringAsync(type));
    }

    private static boolean overridesToString(Type type) {
        if (type instanceof com.sun.jdi.ClassType) {

            Method toStringMethod = DebuggerUtils.findMethod((ReferenceType) type, "toString", "()Ljava/lang/String;");

            return (toStringMethod != null &&
                    !"java.lang.Object".equals(toStringMethod
                            .declaringType().name()));
        }
        return false;
    }

    private static CompletableFuture<Boolean> overridesToStringAsync(Type type) {
        if (!Registry.is("debugger.async.jdi")) {
            return CompletableFuture.completedFuture(Boolean.valueOf(overridesToString(type)));
        }
        if (type instanceof com.sun.jdi.ClassType) {
//            return DebuggerUtilsAsync.findAnyBaseType(type,
//                            t -> (t instanceof ReferenceType) ?
//                                    DebuggerUtilsAsync.methods((ReferenceType) t).thenApply(()) :
//                                    CompletableFuture.completedFuture(Boolean.valueOf(false)))
//
//
//                    .thenApply(t -> Boolean.valueOf((t != null)));
        }
        return CompletableFuture.completedFuture(Boolean.valueOf(false));
    }

    public String getUniqueId() {
        return "ToStringRenderer";
    }

    public String getName() {
        return "toString";
    }

    public void setName(String name) {
    }

    public InsidiousToStringRenderer clone() {
        InsidiousToStringRenderer cloned = (InsidiousToStringRenderer) super.clone();


        ClassFilter[] classFilters = (this.myClassFilters.length > 0) ? new ClassFilter[this.myClassFilters.length] : ClassFilter.EMPTY_ARRAY;
        for (int idx = 0; idx < classFilters.length; idx++) {
            classFilters[idx] = this.myClassFilters[idx].clone();
        }
        cloned.myClassFilters = classFilters;
        return cloned;
    }

    public String calcLabel(InsidiousValueDescriptor valueDescriptor, EvaluationContext evaluationContext, DescriptorLabelListener labelListener) throws EvaluateException {
        if (!isShowValue(valueDescriptor, evaluationContext)) {
            return "";
        }

        Value value = valueDescriptor.getValue();
        if (value instanceof ObjectReference) {
            EvaluatorUtil.ensureNotInsideObjectConstructor((ObjectReference) value, evaluationContext);
        }

        String valueAsString = EvaluatorUtil.getValueAsString(evaluationContext, value);
        valueDescriptor.setValueLabel(StringUtil.notNullize(valueAsString));
        labelListener.labelChanged();
        return valueAsString;
    }

    @NotNull
    public String getLinkText() {
        return DebuggerBundle.message("message.node.toString");
    }

    public boolean isUseClassFilters() {
        return this.USE_CLASS_FILTERS;
    }

    public void setUseClassFilters(boolean value) {
        this.USE_CLASS_FILTERS = value;
    }

    public boolean isOnDemand(EvaluationContext evaluationContext, InsidiousValueDescriptor valueDescriptor) {
        if (this.ON_DEMAND || (this.USE_CLASS_FILTERS && !isFiltered(valueDescriptor.getType()))) {
            return true;
        }
        return false;
//        return super.isOnDemand(evaluationContext, valueDescriptor);
    }

    public void buildChildren(Value value, InsidiousChildrenBuilder builder, EvaluationContext evaluationContext) {
        RendererManager.getInstance()
                .getDefaultRenderer(value.type())
                .buildChildren(value, builder, evaluationContext);
    }


    public PsiElement getChildValueExpression(InsidiousDebuggerTreeNode node, EvaluationContext context) throws EvaluateException {
        return RendererManager.getInstance()
                .getDefaultRenderer(((InsidiousValueDescriptor) node
                        .getParent().getDescriptor()).getType())
                .getChildValueExpression(node, context);
    }


    public CompletableFuture<Boolean> isExpandableAsync(Value value, EvaluationContext evaluationContext, NodeDescriptor parentDescriptor) {
        return RendererManager.getInstance()
                .getDefaultRenderer(value.type())
                .isExpandableAsync(value, evaluationContext, parentDescriptor);
    }


    public void readExternal(Element element) {
        super.readExternal(element);

        this.ON_DEMAND = Boolean.parseBoolean(JDOMExternalizerUtil.readField(element, "ON_DEMAND"));
        this
                .USE_CLASS_FILTERS = Boolean.parseBoolean(JDOMExternalizerUtil.readField(element, "USE_CLASS_FILTERS"));
        this.myClassFilters = DebuggerUtilsEx.readFilters(element.getChildren("filter"));
    }


    public void writeExternal(@NotNull Element element) {
        super.writeExternal(element);

        if (this.ON_DEMAND) {
            JDOMExternalizerUtil.writeField(element, "ON_DEMAND", "true");
        }
        if (this.USE_CLASS_FILTERS) {
            JDOMExternalizerUtil.writeField(element, "USE_CLASS_FILTERS", "true");
        }
        DebuggerUtilsEx.writeFilters(element, "filter", this.myClassFilters);
    }

    public ClassFilter[] getClassFilters() {
        return this.myClassFilters;
    }

    public void setClassFilters(ClassFilter[] classFilters) {
        this.myClassFilters = (classFilters != null) ? classFilters : ClassFilter.EMPTY_ARRAY;
    }

    private boolean isFiltered(Type t) {
        if (t instanceof ReferenceType) {
            for (ClassFilter classFilter : this.myClassFilters) {
                if (classFilter.isEnabled() &&
                        DebuggerUtils.instanceOf(t, classFilter.getPattern())) {
                    return true;
                }
            }
        }
        return DebuggerUtilsEx.isFiltered(t.name(), this.myClassFilters);
    }

    public boolean isOnDemand() {
        return this.ON_DEMAND;
    }

    public void setOnDemand(boolean value) {
        this.ON_DEMAND = value;
    }


    public boolean hasOverhead() {
        return true;
    }


    public boolean isApplicable(Type type) {
        if (!(type instanceof ReferenceType)) {
            return false;
        }

        if ("java.lang.String".equals(type.name())) {
            return false;
        }

        return overridesToString(type);
    }
}


