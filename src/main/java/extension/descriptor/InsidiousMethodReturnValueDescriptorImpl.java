package extension.descriptor;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiExpression;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.Method;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import extension.descriptor.renderer.RendererManager;
import extension.evaluation.EvaluationContext;
import org.jetbrains.annotations.NotNull;

public class InsidiousMethodReturnValueDescriptorImpl extends InsidiousValueDescriptorImpl {
    private final Method myMethod;

    public InsidiousMethodReturnValueDescriptorImpl(Project project, @NotNull Method method, Value value) {
        super(project, value);
        this.myMethod = method;
    }


    public Value calcValue(EvaluationContext evaluationContext) throws EvaluateException {
        return getValue();
    }

    @NotNull
    public Method getMethod() {
        return this.myMethod;
    }


    public String getName() {
        return RendererManager.getInstance()
                .getClassRenderer()
                .renderTypeName(this.myMethod.declaringType().name()) + "." +

                DebuggerUtilsEx.methodNameWithArguments(this.myMethod);
    }


    public Type getType() {
        Type type = super.getType();
        if (type == null) {
            try {
                type = this.myMethod.returnType();
            } catch (ClassNotLoadedException classNotLoadedException) {
            }
        }

        return type;
    }


    public PsiExpression getDescriptorEvaluation(EvaluationContext context) throws EvaluateException {
        return null;
    }


    public boolean canSetValue() {
        return false;
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\descriptor\InsidiousMethodReturnValueDescriptorImpl.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */