package extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.jdi.VirtualMachineProxy;
import com.intellij.openapi.util.text.StringUtil;
import com.sun.jdi.Value;
import extension.connector.InsidiousConnector;
import extension.evaluation.EvaluationContext;
import extension.evaluation.EvaluatorUtil;

import java.util.ArrayList;
import java.util.List;

public class ExpressionListEvaluator
        implements Evaluator {
    private final List<? extends Evaluator> myEvaluators;

    public ExpressionListEvaluator(List<? extends Evaluator> evaluators) {
        this.myEvaluators = evaluators;
    }


    public Object evaluate(EvaluationContext context) throws EvaluateException {
        List<String> strings = new ArrayList<>(this.myEvaluators.size());
        for (Evaluator evaluator : this.myEvaluators) {
            strings.add(
                    EvaluatorUtil.getValueAsString(context, (Value) evaluator.evaluate(context)));
        }
        VirtualMachineProxy proxy = context.getStackFrameProxy().getVirtualMachine();
        if (proxy instanceof InsidiousConnector) {
            InsidiousConnector connector = (InsidiousConnector) proxy;
            return connector.createString(StringUtil.join(strings, ", "));
        }
        return null;
    }
}


