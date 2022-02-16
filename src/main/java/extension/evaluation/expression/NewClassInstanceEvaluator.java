package extension.evaluation.expression;

import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ArrayUtil;
import com.sun.jdi.ClassType;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;
import extension.DebuggerBundle;
import extension.evaluation.EvaluationContext;
import extension.evaluation.EvaluatorUtil;
import extension.evaluation.JVMName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


class NewClassInstanceEvaluator
        implements Evaluator {
    private static final Logger LOG = Logger.getInstance(NewClassInstanceEvaluator.class);

    private final TypeEvaluator myClassTypeEvaluator;

    private final JVMName myConstructorSignature;

    private final Evaluator[] myParamsEvaluators;


    NewClassInstanceEvaluator(TypeEvaluator classTypeEvaluator, JVMName constructorSignature, Evaluator[] argumentEvaluators) {
        this.myClassTypeEvaluator = classTypeEvaluator;
        this.myConstructorSignature = constructorSignature;
        this.myParamsEvaluators = argumentEvaluators;
    }

    public Object evaluate(EvaluationContext context) throws EvaluateException {
        List<Value> arguments;
        ObjectReference objRef;
        Object obj = this.myClassTypeEvaluator.evaluate(context);
        if (!(obj instanceof ClassType)) {
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.cannot.evaluate.class.type"));
        }
        ClassType classType = (ClassType) obj;


        Method method = DebuggerUtils.findMethod(classType, "<init>", this.myConstructorSignature


                .getName(context.getVirtualMachineProxy()));
        if (method == null) {
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.cannot.resolve.constructor", this.myConstructorSignature));
        }


        if (!ArrayUtil.isEmpty((Object[]) this.myParamsEvaluators)) {
            arguments = new ArrayList<>(this.myParamsEvaluators.length);
            for (Evaluator evaluator : this.myParamsEvaluators) {
                Object res = evaluator.evaluate(context);
                if (!(res instanceof Value) && res != null) {
                    LOG.error("Unable to call newInstance, evaluator " + evaluator + " result is not Value, but " + res);
                }


                arguments.add((Value) res);
            }
        } else {
            arguments = Collections.emptyList();
        }


        try {
            objRef = (ObjectReference) EvaluatorUtil.invokeMethod(context, classType, method, arguments);
        } catch (EvaluateException e) {
            throw EvaluateExceptionUtil.createEvaluateException(e);
        }
        return objRef;
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\evaluation\expression\NewClassInstanceEvaluator.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */