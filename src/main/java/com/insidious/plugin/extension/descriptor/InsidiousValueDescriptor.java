package com.insidious.plugin.extension.descriptor;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.ui.tree.NodeDescriptor;
import com.intellij.psi.PsiElement;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.impl.ui.tree.ValueMarkup;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public interface InsidiousValueDescriptor extends NodeDescriptor {
    PsiElement getDescriptorEvaluation(EvaluationContext paramEvaluationContext) throws EvaluateException;

    Value getValue();

    @Nullable
    default Type getType() {
        Value value = getValue();
        return (value != null) ? value.type() : null;
    }

    void setValueLabel(String paramString);

    void setIdLabel(String paramString);

    String setValueLabelFailed(EvaluateException paramEvaluateException);

    Icon setValueIcon(Icon paramIcon);

    boolean isArray();

    boolean isLvalue();

    boolean isNull();

    boolean isPrimitive();

    boolean isString();

    @Nullable
    ValueMarkup getMarkup(XDebugProcess paramXDebugProcess);

    void setMarkup(XDebugProcess paramXDebugProcess, @Nullable ValueMarkup paramValueMarkup);
}