package extension.descriptor;

import com.intellij.xdebugger.frame.XFullValueEvaluator;
import extension.evaluation.EvaluationContext;
import org.jetbrains.annotations.Nullable;

public interface InsidiousFullValueEvaluatorProvider {
    @Nullable
    XFullValueEvaluator getFullValueEvaluator(EvaluationContext paramEvaluationContext, InsidiousValueDescriptorImpl paramInsidiousValueDescriptorImpl);
}

