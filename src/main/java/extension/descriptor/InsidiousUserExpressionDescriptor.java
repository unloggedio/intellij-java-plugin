package extension.descriptor;


import extension.evaluation.EvaluationContext;

public interface InsidiousUserExpressionDescriptor extends InsidiousValueDescriptor {
    void setContext(EvaluationContext paramEvaluationContext);
}

