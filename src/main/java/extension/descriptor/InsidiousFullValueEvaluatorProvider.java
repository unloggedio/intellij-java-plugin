package extension.descriptor;

import com.intellij.xdebugger.frame.XFullValueEvaluator;
import extension.evaluation.EvaluationContext;
import org.jetbrains.annotations.Nullable;

public interface InsidiousFullValueEvaluatorProvider {
    @Nullable
    XFullValueEvaluator getFullValueEvaluator(EvaluationContext paramEvaluationContext, InsidiousValueDescriptorImpl paramInsidiousValueDescriptorImpl);
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\descriptor\InsidiousFullValueEvaluatorProvider.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */