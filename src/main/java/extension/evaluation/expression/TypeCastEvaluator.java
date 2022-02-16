package extension.evaluation.expression;

import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.impl.DebuggerUtilsImpl;
import com.sun.jdi.*;
import extension.DebuggerBundle;
import extension.evaluation.EvaluationContext;
import extension.evaluation.EvaluatorUtil;
import org.jetbrains.annotations.NotNull;

public class TypeCastEvaluator
        implements Evaluator {
    private final Evaluator myOperandEvaluator;
    private final String myPrimitiveCastType;
    private final TypeEvaluator myTypeCastEvaluator;

    public TypeCastEvaluator(Evaluator operandEvaluator, @NotNull TypeEvaluator typeCastEvaluator) {
        this.myOperandEvaluator = operandEvaluator;
        this.myPrimitiveCastType = null;
        this.myTypeCastEvaluator = typeCastEvaluator;
    }

    public TypeCastEvaluator(Evaluator operandEvaluator, @NotNull String primitiveType) {
        this.myOperandEvaluator = operandEvaluator;
        this.myPrimitiveCastType = primitiveType;
        this.myTypeCastEvaluator = null;
    }


    public Object evaluate(EvaluationContext context) throws EvaluateException {
        Value value = (Value) this.myOperandEvaluator.evaluate(context);
        if (value == null) {
            if (this.myPrimitiveCastType != null) {
                throw EvaluateExceptionUtil.createEvaluateException(
                        DebuggerBundle.message("evaluation.error.cannot.cast.null", new Object[]{this.myPrimitiveCastType}));
            }

            return null;
        }
        VirtualMachine vm = context.getVirtualMachineProxy().getVirtualMachine();
        if (DebuggerUtils.isInteger(value)) {

            value = EvaluatorUtil.createValue(vm, this.myPrimitiveCastType, ((PrimitiveValue) value)
                    .longValue());
            if (value == null) {
                throw EvaluateExceptionUtil.createEvaluateException(
                        DebuggerBundle.message("evaluation.error.cannot.cast.numeric", new Object[]{this.myPrimitiveCastType}));
            }
        } else if (DebuggerUtils.isNumeric(value)) {

            value = EvaluatorUtil.createValue(vm, this.myPrimitiveCastType, ((PrimitiveValue) value)
                    .doubleValue());
            if (value == null) {
                throw EvaluateExceptionUtil.createEvaluateException(
                        DebuggerBundle.message("evaluation.error.cannot.cast.numeric", new Object[]{this.myPrimitiveCastType}));
            }
        } else if (value instanceof BooleanValue) {

            value = EvaluatorUtil.createValue(vm, this.myPrimitiveCastType, ((BooleanValue) value)
                    .booleanValue());
            if (value == null) {
                throw EvaluateExceptionUtil.createEvaluateException(
                        DebuggerBundle.message("evaluation.error.cannot.cast.boolean", new Object[]{this.myPrimitiveCastType}));
            }
        } else if (value instanceof CharValue) {

            value = EvaluatorUtil.createValue(vm, this.myPrimitiveCastType, ((CharValue) value)
                    .charValue());
            if (value == null) {
                throw EvaluateExceptionUtil.createEvaluateException(
                        DebuggerBundle.message("evaluation.error.cannot.cast.char", new Object[]{this.myPrimitiveCastType}));
            }
        } else if (value instanceof ObjectReference) {
            ReferenceType type = ((ObjectReference) value).referenceType();
            if (this.myTypeCastEvaluator == null) {
                throw EvaluateExceptionUtil.createEvaluateException(
                        DebuggerBundle.message("evaluation.error.cannot.cast.object", new Object[]{

                                type.name(), this.myPrimitiveCastType
                        }));
            }
            ReferenceType castType = this.myTypeCastEvaluator.evaluate(context);
            if (!DebuggerUtilsImpl.instanceOf(type, castType)) {
                throw EvaluateExceptionUtil.createEvaluateException(
                        DebuggerBundle.message("evaluation.error.cannot.cast.object", new Object[]{

                                type.name(), castType
                                .name()
                        }));
            }
        }
        return value;
    }
}

