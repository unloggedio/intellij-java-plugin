package extension.evaluation.expression;

import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.engine.evaluation.expression.Modifier;
import com.intellij.openapi.project.Project;
import com.sun.jdi.*;
import extension.DebuggerBundle;
import extension.descriptor.InsidiousArrayElementDescriptorImpl;
import extension.evaluation.EvaluationContext;
import extension.evaluation.InsidiousNodeDescriptorImpl;


class ArrayAccessEvaluator
        implements Evaluator {
    private final Evaluator myArrayReferenceEvaluator;
    private final Evaluator myIndexEvaluator;
    private ArrayReference myEvaluatedArrayReference;
    private int myEvaluatedIndex;

    ArrayAccessEvaluator(Evaluator arrayReferenceEvaluator, Evaluator indexEvaluator) {
        this.myArrayReferenceEvaluator = arrayReferenceEvaluator;
        this.myIndexEvaluator = indexEvaluator;
    }


    public Object evaluate(EvaluationContext context) throws EvaluateException {
        this.myEvaluatedIndex = 0;
        this.myEvaluatedArrayReference = null;
        Value indexValue = (Value) this.myIndexEvaluator.evaluate(context);
        Value arrayValue = (Value) this.myArrayReferenceEvaluator.evaluate(context);
        if (!(arrayValue instanceof ArrayReference)) {
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.array.reference.expected"));
        }
        this.myEvaluatedArrayReference = (ArrayReference) arrayValue;
        if (!DebuggerUtils.isInteger(indexValue)) {
            throw EvaluateExceptionUtil.createEvaluateException(
                    DebuggerBundle.message("evaluation.error.invalid.index.expression"));
        }
        this.myEvaluatedIndex = ((PrimitiveValue) indexValue).intValue();
        try {
            return this.myEvaluatedArrayReference.getValue(this.myEvaluatedIndex);
        } catch (Exception e) {
            throw EvaluateExceptionUtil.createEvaluateException(e);
        }
    }


    public Modifier getModifier() {
        Modifier modifier = null;
        if (this.myEvaluatedArrayReference != null) {
            modifier = new Modifier() {
                public boolean canInspect() {
                    return true;
                }


                public boolean canSetValue() {
                    return true;
                }


                public void setValue(Value value) throws ClassNotLoadedException, InvalidTypeException {
                    ArrayAccessEvaluator.this.myEvaluatedArrayReference.setValue(ArrayAccessEvaluator.this.myEvaluatedIndex, value);
                }


                public Type getExpectedType() throws EvaluateException {
                    try {
                        ArrayType type = (ArrayType) ArrayAccessEvaluator.this.myEvaluatedArrayReference.referenceType();
                        return type.componentType();
                    } catch (ClassNotLoadedException e) {
                        throw EvaluateExceptionUtil.createEvaluateException(e);
                    }
                }


                public InsidiousNodeDescriptorImpl getInspectItem(Project project) {
                    return new InsidiousArrayElementDescriptorImpl(project, ArrayAccessEvaluator.this
                            .myEvaluatedArrayReference, ArrayAccessEvaluator.this.myEvaluatedIndex);
                }
            };
        }
        return modifier;
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\evaluation\expression\ArrayAccessEvaluator.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */