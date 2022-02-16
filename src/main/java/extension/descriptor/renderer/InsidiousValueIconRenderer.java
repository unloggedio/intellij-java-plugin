package extension.descriptor.renderer;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import extension.descriptor.InsidiousValueDescriptor;
import extension.evaluation.EvaluationContext;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public interface InsidiousValueIconRenderer {
    @Nullable
    Icon calcValueIcon(InsidiousValueDescriptor paramInsidiousValueDescriptor,
                       EvaluationContext paramEvaluationContext,
                       DescriptorLabelListener paramDescriptorLabelListener) throws EvaluateException;
}


