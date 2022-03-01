package com.insidious.plugin.extension.descriptor;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.ui.tree.render.Renderer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.ui.ColoredTextContainer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.xdebugger.frame.presentation.XValuePresentation;
import com.intellij.xdebugger.impl.evaluate.XValueCompactPresentation;
import com.intellij.xdebugger.impl.ui.tree.XValueExtendedPresentation;
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl;
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueTextRendererImpl;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ArrayType;
import com.sun.jdi.Value;
import com.insidious.plugin.extension.descriptor.renderer.InsidiousToStringRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class InsidiousJavaValuePresentation
        extends XValueExtendedPresentation
        implements XValueCompactPresentation {
    protected final InsidiousValueDescriptorImpl myValueDescriptor;

    public InsidiousJavaValuePresentation(InsidiousValueDescriptorImpl valueDescriptor) {
        this.myValueDescriptor = valueDescriptor;
    }

    private static String truncateToMaxLength(@NotNull String value) {
        return value.substring(0, Math.min(value.length(), 1000));
    }

    @Nullable
    public String getType() {
        return this.myValueDescriptor.getIdLabel();
    }

    public void renderValue(@NotNull XValuePresentation.XValueTextRenderer renderer) {
        renderValue(renderer, null);
    }

    public void renderValue(@NotNull XValuePresentation.XValueTextRenderer renderer, @Nullable XValueNodeImpl node) {
        boolean compact = (node != null);
        String valueText = this.myValueDescriptor.getValueText();
        EvaluateException exception = this.myValueDescriptor.getEvaluateException();
        if (exception != null) {
            String errorMessage = exception.getMessage();
            if (valueText.endsWith(errorMessage)) {
                renderer.renderValue(valueText
                        .substring(0, valueText.length() - errorMessage.length()));
            }
            renderer.renderError(errorMessage);
        } else {
            if (compact && node.getValueContainer() instanceof InsidiousJavaValue) {
                InsidiousJavaValue container = (InsidiousJavaValue) node.getValueContainer();

                if (container.getDescriptor().isArray()) {

                    ArrayReference arrayReference = (ArrayReference) container.getDescriptor().getValue();
                    ArrayType type = (ArrayType) container.getDescriptor().getType();
                    if (type != null) {
                        String typeName = type.componentTypeName();
                        if (TypeConversionUtil.isPrimitive(typeName) || "java.lang.String"
                                .equals(typeName)) {
                            int size = arrayReference.length();

                            int max = Math.min(size,

                                    "java.lang.String".equals(typeName) ?
                                            5 :
                                            10);


                            List<Value> values = arrayReference.getValues(0, max);
                            int i = 0;
                            List<String> vals = new ArrayList<>(max);
                            while (i < values.size()) {
                                vals.add(StringUtil.first(values.get(i).toString(), 15, true));
                                i++;
                            }
                            String more = "";
                            if (vals.size() < size) {
                                more = ", + " + (size - vals.size()) + " more";
                            }

                            renderer.renderValue("{" + StringUtil.join(vals, ", ") + more + "}");

                            return;
                        }
                    }
                }
            }
            if (this.myValueDescriptor.isString()) {
                renderer.renderStringValue(valueText, "\"", 1000);

                return;
            }
            String value = truncateToMaxLength(valueText);
            Renderer lastRenderer = this.myValueDescriptor.getLastLabelRenderer();
            if (lastRenderer instanceof InsidiousToStringRenderer) {
                value = StringUtil.wrapWithDoubleQuote(value);
            }
            renderer.renderValue(value);
        }
    }

    @NotNull
    public String getSeparator() {
        boolean emptyAfterSeparator = (!this.myValueDescriptor.isShowIdLabel() && isValueEmpty());
        String declaredType = this.myValueDescriptor.getDeclaredTypeLabel();
        if (!StringUtil.isEmpty(declaredType)) {
            return emptyAfterSeparator ? declaredType : (declaredType + " " + " = ");
        }
        return emptyAfterSeparator ? "" : " = ";
    }

    public boolean isModified() {
        return this.myValueDescriptor.isDirty();
    }

    private boolean isValueEmpty() {
        MyEmptyContainerChecker checker = new MyEmptyContainerChecker();

        renderValue(new XValueTextRendererImpl(checker));
        return checker.isEmpty;
    }

    private static class MyEmptyContainerChecker
            implements ColoredTextContainer {
        boolean isEmpty = true;

        private MyEmptyContainerChecker() {
        }

        public void append(@NotNull String fragment, @NotNull SimpleTextAttributes attributes) {
            if (!fragment.isEmpty()) this.isEmpty = false;
        }

        public void setIcon(@Nullable Icon icon) {
        }

        public void setToolTipText(@Nullable String text) {
        }

        public void append(@NotNull String fragment, @NotNull SimpleTextAttributes attributes, Object tag) {
            append(fragment, attributes);
        }
    }
}

