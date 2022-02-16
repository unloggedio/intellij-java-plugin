package extension.evaluation.expression;

import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.openapi.util.registry.Registry;
import com.sun.jdi.Method;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import extension.DebuggerBundle;
import extension.InsidiousVirtualMachineProxy;
import extension.evaluation.EvaluationContext;
import extension.evaluation.EvaluatorUtil;

import java.util.Collections;


class LiteralEvaluator
        implements Evaluator {
    private final Object myValue;
    private final String myExpectedType;

    LiteralEvaluator(Object value, String expectedType) {
        this.myValue = value;
        this.myExpectedType = expectedType;
    }


    public Object evaluate(EvaluationContext context) throws EvaluateException {
        if (this.myValue == null) {
            return null;
        }
        VirtualMachine vm = context.getVirtualMachineProxy().getVirtualMachine();
        if (this.myValue instanceof Boolean) {
            return EvaluatorUtil.createValue(vm, this.myExpectedType, ((Boolean) this.myValue)
                    .booleanValue());
        }
        if (this.myValue instanceof Character) {
            return EvaluatorUtil.createValue(vm, this.myExpectedType, ((Character) this.myValue).charValue());
        }
        if (this.myValue instanceof Double) {
            return EvaluatorUtil.createValue(vm, this.myExpectedType, ((Number) this.myValue).doubleValue());
        }
        if (this.myValue instanceof Float) {
            return EvaluatorUtil.createValue(vm, this.myExpectedType, ((Number) this.myValue).floatValue());
        }
        if (this.myValue instanceof Number) {
            return EvaluatorUtil.createValue(vm, this.myExpectedType, ((Number) this.myValue).longValue());
        }
        if (this.myValue instanceof String) {
            StringReference str = vm.mirrorOf((String) this.myValue);
            InsidiousVirtualMachineProxy vmProxy = context.getVirtualMachineProxy();

            if (Registry.is("debugger.intern.string.literals") && vmProxy.versionHigher("1.7")) {

                Method internMethod = DebuggerUtils.findMethod(str
                        .referenceType(), "intern", "()Ljava/lang/String;");
                if (internMethod != null) {

                    ThreadReference thread = context.getStackFrameProxy().threadProxy().getThreadReference();
                    try {
                        return str.invokeMethod(thread, internMethod,


                                Collections.emptyList(), 1);
                    } catch (Exception e) {
                        throw new EvaluateException(e.getMessage(), e);
                    }
                }
            }
            return str;
        }
        throw EvaluateExceptionUtil.createEvaluateException(
                DebuggerBundle.message("evaluation.error.unknown.expression.type", this.myExpectedType));
    }


    public String toString() {
        return (this.myValue != null) ? this.myValue.toString() : "null";
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\evaluation\expression\LiteralEvaluator.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */