package extension.descriptor.renderer;

import com.intellij.debugger.actions.ForceOnDemandRenderersAction;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.frame.XFullValueEvaluator;
import com.intellij.xdebugger.frame.XValue;
import com.intellij.xdebugger.frame.XValueNode;
import com.intellij.xdebugger.frame.XValuePlace;
import com.intellij.xdebugger.impl.XDebugSessionImpl;
import com.intellij.xdebugger.impl.ui.tree.nodes.HeadlessValueEvaluationCallback;
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl;
import extension.descriptor.InsidiousFullValueEvaluatorProvider;
import extension.descriptor.InsidiousJavaValue;
import extension.descriptor.InsidiousValueDescriptor;
import extension.descriptor.InsidiousValueDescriptorImpl;
import extension.evaluation.EvaluationContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface InsidiousOnDemandRenderer extends InsidiousFullValueEvaluatorProvider {
    public static final Key<Boolean> ON_DEMAND_CALCULATED = Key.create("ON_DEMAND_CALCULATED");

    static XFullValueEvaluator createFullValueEvaluator(String text) {
        return (new XFullValueEvaluator(text) {
            public void startEvaluation(@NotNull XFullValueEvaluator.XFullValueEvaluationCallback callback) {
                if (callback instanceof HeadlessValueEvaluationCallback) {
                    XValueNodeImpl node = ((HeadlessValueEvaluationCallback) callback).getNode();
                    node.clearFullValueEvaluator();
                    InsidiousOnDemandRenderer.setCalculated((InsidiousValueDescriptor) ((InsidiousJavaValue) node.getValueContainer()).getDescriptor());
                    ((XValue) node.getValueContainer()).computePresentation((XValueNode) node, XValuePlace.TREE);
                }
                callback.evaluated("");
            }
        }).setShowValuePopup(false);
    }

    static boolean isCalculated(InsidiousValueDescriptor descriptor) {
        return ((Boolean) ON_DEMAND_CALCULATED.get((UserDataHolder) descriptor, Boolean.valueOf(false))).booleanValue();
    }

    static void setCalculated(InsidiousValueDescriptor descriptor) {
        ON_DEMAND_CALCULATED.set((UserDataHolder) descriptor, Boolean.valueOf(true));
    }

    static boolean isOnDemandForced(XDebugProcess process) {
        return (process != null &&
                ForceOnDemandRenderersAction.isForcedOnDemand((XDebugSessionImpl) process
                        .getSession()));
    }

    @Nullable
    default XFullValueEvaluator getFullValueEvaluator(EvaluationContext evaluationContext, InsidiousValueDescriptorImpl valueDescriptor) {
        if (isOnDemand(evaluationContext, (InsidiousValueDescriptor) valueDescriptor) && !isCalculated((InsidiousValueDescriptor) valueDescriptor)) {
            return createFullValueEvaluator(getLinkText());
        }
        return null;
    }

    String getLinkText();

    default boolean isOnDemand(EvaluationContext evaluationContext, InsidiousValueDescriptor valueDescriptor) {
        if (evaluationContext == null) {
            return false;
        }
        return isOnDemandForced(evaluationContext.getVirtualMachineProxy().getXDebugProcess());
    }

    default boolean isShowValue(InsidiousValueDescriptor valueDescriptor, EvaluationContext evaluationContext) {
        return (!isOnDemand(evaluationContext, valueDescriptor) || isCalculated(valueDescriptor));
    }
}


