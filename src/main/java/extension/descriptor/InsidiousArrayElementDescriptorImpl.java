package extension.descriptor;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.jdi.VirtualMachineProxyImpl;
import com.intellij.debugger.ui.tree.ArrayElementDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpression;
import com.intellij.util.IncorrectOperationException;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.frame.XValueModifier;
import com.sun.jdi.*;
import extension.DebuggerBundle;
import extension.evaluation.EvaluationContext;
import extension.evaluation.EvaluatorUtil;
import org.jetbrains.annotations.NotNull;

public class InsidiousArrayElementDescriptorImpl extends InsidiousValueDescriptorImpl implements ArrayElementDescriptor {
    private final int myIndex;
    private final ArrayReference myArray;
    private Ref<Value> myPresetValue;

    public InsidiousArrayElementDescriptorImpl(Project project, ArrayReference array, int index) {
        super(project);
        this.myArray = array;
        this.myIndex = index;
        setLvalue(true);
    }

    public static Value getArrayElement(ArrayReference reference, int idx) throws EvaluateException {
        try {
            return reference.getValue(idx);
        } catch (ObjectCollectedException e) {
            throw EvaluateExceptionUtil.ARRAY_WAS_COLLECTED;
        }
    }

    public int getIndex() {
        return this.myIndex;
    }

    public ArrayReference getArray() {
        return this.myArray;
    }

    public String getName() {
        return String.valueOf(this.myIndex);
    }

    public void setValue(Value value) {
        this.myPresetValue = Ref.create(value);
    }

    public Value calcValue(EvaluationContext evaluationContext) throws EvaluateException {
        if (this.myPresetValue != null) {
            return this.myPresetValue.get();
        }
        return getArrayElement(this.myArray, this.myIndex);
    }

    public PsiExpression getDescriptorEvaluation(EvaluationContext context) throws EvaluateException {
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(this.myProject);
        try {
            return elementFactory.createExpressionFromText("this[" + this.myIndex + "]", null);
        } catch (IncorrectOperationException e) {
            throw new EvaluateException(e.getMessage(), e);
        }
    }


    public XValueModifier getModifier(InsidiousJavaValue value) {
        return new InsidiousJavaValueModifier(value) {
            protected void setValueImpl(@NotNull XExpression expression, @NotNull XValueModifier.XModificationCallback callback) {
                final InsidiousArrayElementDescriptorImpl elementDescriptor = InsidiousArrayElementDescriptorImpl.this;

                final ArrayReference array = elementDescriptor.getArray();
                if (array != null) {
                    if (VirtualMachineProxyImpl.isCollected(array)) {


                        Messages.showWarningDialog(InsidiousArrayElementDescriptorImpl.this
                                        .getProject(),
                                DebuggerBundle.message("evaluation.error.array.collected") + "\n" +

                                        DebuggerBundle.message("warning.recalculate"),
                                DebuggerBundle.message("title.set.value"));

                        return;
                    }
                    final ArrayType arrType = (ArrayType) array.referenceType();
                    set(expression, callback, InsidiousArrayElementDescriptorImpl.this


                            .getProject(), new InsidiousJavaValueModifier.SetValueRunnable() {


                        public void setValue(EvaluationContext evaluationContext, Value newValue) throws ClassNotLoadedException, InvalidTypeException, EvaluateException {
                            array.setValue(elementDescriptor
                                            .getIndex(),

                                    EvaluatorUtil.preprocessValue(InsidiousArrayElementDescriptorImpl.this.myStoredEvaluationContext, newValue,


                                            getLType()));
                            InsidiousJavaValueModifier.update(evaluationContext);
                        }


                        public ClassLoaderReference getClassLoader(EvaluationContext evaluationContext) {
                            return arrType.classLoader();
                        }


                        @NotNull
                        public Type getLType() throws ClassNotLoadedException {
                            return arrType.componentType();
                        }
                    });
                }
            }
        };
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\descriptor\InsidiousArrayElementDescriptorImpl.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */