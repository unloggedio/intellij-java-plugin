package com.insidious.plugin.extension.descriptor;

import com.intellij.xdebugger.frame.XFullValueEvaluator;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import org.jetbrains.annotations.Nullable;

public interface InsidiousFullValueEvaluatorProvider {
    @Nullable
    XFullValueEvaluator getFullValueEvaluator(EvaluationContext paramEvaluationContext, InsidiousValueDescriptorImpl paramInsidiousValueDescriptorImpl);
}

