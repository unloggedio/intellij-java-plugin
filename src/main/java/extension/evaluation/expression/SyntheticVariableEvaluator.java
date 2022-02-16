package extension.evaluation.expression;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.engine.evaluation.expression.Modifier;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import extension.DebuggerBundle;
import extension.evaluation.EvaluationContext;
import extension.evaluation.InsidiousNodeDescriptorImpl;
import extension.evaluation.JVMName;
import org.jetbrains.annotations.Nullable;

public class SyntheticVariableEvaluator
        implements Evaluator {
    private static final Logger LOG = Logger.getInstance(SyntheticVariableEvaluator.class);

    private final CodeFragmentEvaluator myCodeFragmentEvaluator;

    private final String myLocalName;

    private final JVMName myTypeName;

    private String myTypeNameString;

    public SyntheticVariableEvaluator(CodeFragmentEvaluator codeFragmentEvaluator, String localName, @Nullable JVMName typeName) {
        this.myCodeFragmentEvaluator = codeFragmentEvaluator;
        this.myLocalName = localName;
        this.myTypeName = typeName;
    }


    public Object evaluate(EvaluationContext context) throws EvaluateException {
        if (this.myTypeNameString == null && this.myTypeName != null) {
            this.myTypeNameString = this.myTypeName.getName(context.getVirtualMachineProxy());
        }
        return this.myCodeFragmentEvaluator.getValue(this.myLocalName, context.getVirtualMachineProxy());
    }


    public Modifier getModifier() {
        return new Modifier() {
            public boolean canInspect() {
                return false;
            }


            public boolean canSetValue() {
                return false;
            }


            public void setValue(Value value) throws EvaluateException {
                if (value != null) {
                    Type type = value.type();
                    if (SyntheticVariableEvaluator.this.myTypeName != null &&
                            !DebuggerUtilsEx.isAssignableFrom(SyntheticVariableEvaluator.this.myTypeNameString, type))
                        throw EvaluateExceptionUtil.createEvaluateException(
                                DebuggerBundle.message("evaluation.error.cannot.cast.object", type.name(),
                                        "the other type"));
                }
                SyntheticVariableEvaluator.this.myCodeFragmentEvaluator.setValue(SyntheticVariableEvaluator.this.myLocalName, value);
            }


            public Type getExpectedType() {
                SyntheticVariableEvaluator.LOG.assertTrue(false);
                return null;
            }


            public InsidiousNodeDescriptorImpl getInspectItem(Project project) {
                return null;
            }
        };
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\evaluation\expression\SyntheticVariableEvaluator.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */