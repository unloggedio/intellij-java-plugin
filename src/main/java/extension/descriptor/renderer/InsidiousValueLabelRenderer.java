package extension.descriptor.renderer;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.intellij.debugger.ui.tree.render.Renderer;
import extension.descriptor.InsidiousValueDescriptor;
import extension.evaluation.EvaluationContext;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


public interface InsidiousValueLabelRenderer extends Renderer, InsidiousValueIconRenderer {
    String calcLabel(InsidiousValueDescriptor paramInsidiousValueDescriptor, EvaluationContext paramEvaluationContext, DescriptorLabelListener paramDescriptorLabelListener) throws EvaluateException;

    @Nullable
    default Icon calcValueIcon(InsidiousValueDescriptor descriptor, EvaluationContext evaluationContext, DescriptorLabelListener listener) throws EvaluateException {
        return null;
    }
}


