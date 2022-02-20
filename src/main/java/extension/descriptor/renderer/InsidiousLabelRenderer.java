package extension.descriptor.renderer;

import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.engine.evaluation.TextWithImports;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.sun.jdi.Value;
import extension.DebuggerBundle;
import extension.thread.InsidiousVirtualMachineProxy;
import extension.descriptor.InsidiousValueDescriptor;
import extension.evaluation.EvaluationContext;
import extension.evaluation.EvaluatorUtil;
import extension.evaluation.expression.ExpressionEvaluator;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class InsidiousLabelRenderer extends InsidiousReferenceRenderer implements InsidiousValueLabelRenderer, InsidiousOnDemandRenderer {
    @NonNls
    public static final String UNIQUE_ID = "LabelRenderer";
    public boolean ON_DEMAND;
    private InsidiousCachedEvaluator myLabelExpression = createCachedEvaluator();


    public String getUniqueId() {
        return "LabelRenderer";
    }


    public InsidiousLabelRenderer clone() {
        InsidiousLabelRenderer clone = (InsidiousLabelRenderer) super.clone();
        clone.myLabelExpression = createCachedEvaluator();
        clone.setLabelExpression(getLabelExpression());
        return clone;
    }


    public String calcLabel(InsidiousValueDescriptor descriptor, EvaluationContext evaluationContext, DescriptorLabelListener labelListener) throws EvaluateException {
        String result;
        if (!isShowValue(descriptor, evaluationContext)) {
            return "";
        }

        Value value = descriptor.getValue();


        InsidiousVirtualMachineProxy virtualMachineProxy = evaluationContext.getVirtualMachineProxy();
        Project project = EvaluatorUtil.getProject(evaluationContext);
        if (value != null) {

            try {
                ExpressionEvaluator evaluator = this.myLabelExpression.getEvaluator(project, evaluationContext);

                if (!virtualMachineProxy.isAttached()) {
                    throw EvaluateExceptionUtil.PROCESS_EXITED;
                }

                EvaluationContext thisEvaluationContext = evaluationContext.createEvaluationContext(value);
                Value labelValue = evaluator.evaluate(thisEvaluationContext);
                result = EvaluatorUtil.getValueAsString(thisEvaluationContext, labelValue);
            } catch (EvaluateException ex) {
                throw new EvaluateException(
                        DebuggerBundle.message("error.unable.to.evaluate.expression") + " " + ex

                                .getMessage(), ex);
            }
        } else {

            result = "null";
        }
        return result;
    }


    @NotNull
    public String getLinkText() {
        return "… " + getLabelExpression().getText();
    }


    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);
        DefaultJDOMExternalizer.readExternal(this, element);

        TextWithImports labelExpression = DebuggerUtils.getInstance().readTextWithImports(element, "LABEL_EXPRESSION");
        if (labelExpression != null) {
            setLabelExpression(labelExpression);
        }
    }


    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);
        DefaultJDOMExternalizer.writeExternal(this, element);
        DebuggerUtils.getInstance()
                .writeTextWithImports(element, "LABEL_EXPRESSION", getLabelExpression());
    }

    public TextWithImports getLabelExpression() {
        return this.myLabelExpression.getReferenceExpression();
    }

    public void setLabelExpression(TextWithImports expression) {
        this.myLabelExpression.setReferenceExpression(expression);
    }


    public boolean isOnDemand(EvaluationContext evaluationContext, InsidiousValueDescriptor valueDescriptor) {
        return this.ON_DEMAND;
//        return (this.ON_DEMAND || super.isOnDemand(evaluationContext, valueDescriptor));
    }

    public boolean isOnDemand() {
        return this.ON_DEMAND;
    }

    public void setOnDemand(boolean value) {
        this.ON_DEMAND = value;
    }
}


