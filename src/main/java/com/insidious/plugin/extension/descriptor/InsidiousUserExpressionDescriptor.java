package com.insidious.plugin.extension.descriptor;


import com.insidious.plugin.extension.evaluation.EvaluationContext;

public interface InsidiousUserExpressionDescriptor extends InsidiousValueDescriptor {
    void setContext(EvaluationContext paramEvaluationContext);
}

