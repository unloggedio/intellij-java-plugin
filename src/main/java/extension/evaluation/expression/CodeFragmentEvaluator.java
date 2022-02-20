package extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.engine.evaluation.EvaluateRuntimeException;
import com.intellij.openapi.diagnostic.Logger;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import extension.DebuggerBundle;
import extension.thread.InsidiousVirtualMachineProxy;

import java.util.HashMap;
import java.util.Map;


public class CodeFragmentEvaluator
        extends BlockStatementEvaluator {
    private static final Logger LOG = Logger.getInstance(CodeFragmentEvaluator.class);

    private final CodeFragmentEvaluator myParentFragmentEvaluator;
    private final Map<String, Object> mySyntheticLocals = new HashMap<>();

    public CodeFragmentEvaluator(CodeFragmentEvaluator parentFragmentEvaluator) {
        super(null);
        this.myParentFragmentEvaluator = parentFragmentEvaluator;
    }

    public void setStatements(Evaluator[] evaluators) {
        this.myStatements = evaluators;
    }


    public Value getValue(String localName, InsidiousVirtualMachineProxy vmProxy) throws EvaluateException {
        VirtualMachine vm = vmProxy.getVirtualMachine();
        if (!this.mySyntheticLocals.containsKey(localName)) {
            if (this.myParentFragmentEvaluator != null) {
                return this.myParentFragmentEvaluator.getValue(localName, vmProxy);
            }
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.variable.not.declared", localName));
        }


        Object value = this.mySyntheticLocals.get(localName);
        if (value instanceof Value)
            return (Value) value;
        if (value == null)
            return null;
        if (value instanceof Boolean)
            return vm.mirrorOf(((Boolean) value).booleanValue());
        if (value instanceof Byte)
            return vm.mirrorOf(((Byte) value).byteValue());
        if (value instanceof Character)
            return vm.mirrorOf(((Character) value).charValue());
        if (value instanceof Short)
            return vm.mirrorOf(((Short) value).shortValue());
        if (value instanceof Integer)
            return vm.mirrorOf(((Integer) value).intValue());
        if (value instanceof Long)
            return vm.mirrorOf(((Long) value).longValue());
        if (value instanceof Float)
            return vm.mirrorOf(((Float) value).floatValue());
        if (value instanceof Double)
            return vm.mirrorOf(((Double) value).doubleValue());
        if (value instanceof String) {
            return vm.mirrorOf((String) value);
        }
        LOG.error("unknown default initializer type " + value.getClass().getName());
        return null;
    }


    boolean hasValue(String localName) {
        if (!this.mySyntheticLocals.containsKey(localName)) {
            if (this.myParentFragmentEvaluator != null) {
                return this.myParentFragmentEvaluator.hasValue(localName);
            }
            return false;
        }

        return true;
    }


    public void setInitialValue(String localName, Object value) {
        LOG.assertTrue(!(value instanceof Value), "use setValue for jdi values");
        if (hasValue(localName)) {
            throw new EvaluateRuntimeException(
                    EvaluateExceptionUtil.createEvaluateException(
                            DebuggerBundle.message("evaluation.error.variable.already.declared", localName)));
        }

        this.mySyntheticLocals.put(localName, value);
    }

    public void setValue(String localName, Value value) throws EvaluateException {
        if (!this.mySyntheticLocals.containsKey(localName)) {
            if (this.myParentFragmentEvaluator != null) {
                this.myParentFragmentEvaluator.setValue(localName, value);
            } else {
                throw EvaluateExceptionUtil.createEvaluateException(
                        DebuggerBundle.message("evaluation.error.variable.not.declared", localName));
            }
        } else {

            this.mySyntheticLocals.put(localName, value);
        }
    }
}

