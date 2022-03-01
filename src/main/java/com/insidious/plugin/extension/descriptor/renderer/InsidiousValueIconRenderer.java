package com.insidious.plugin.extension.descriptor.renderer;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.insidious.plugin.extension.descriptor.InsidiousValueDescriptor;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public interface InsidiousValueIconRenderer {
    @Nullable
    Icon calcValueIcon(InsidiousValueDescriptor paramInsidiousValueDescriptor,
                       EvaluationContext paramEvaluationContext,
                       DescriptorLabelListener paramDescriptorLabelListener) throws EvaluateException;
}


