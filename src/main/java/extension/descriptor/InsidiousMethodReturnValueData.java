package extension.descriptor;

import com.intellij.debugger.impl.descriptors.data.DescriptorData;
import com.intellij.debugger.impl.descriptors.data.DisplayKey;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.sun.jdi.Method;
import com.sun.jdi.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class InsidiousMethodReturnValueData
        extends DescriptorData<InsidiousMethodReturnValueDescriptorImpl> {
    @Nullable
    private final Value myReturnValue;
    @NotNull
    private final Method myMethod;

    public InsidiousMethodReturnValueData(@NotNull Method method, @Nullable Value returnValue) {
        this.myMethod = method;
        this.myReturnValue = returnValue;
    }

    @Nullable
    public Value getReturnValue() {
        return this.myReturnValue;
    }

    @NotNull
    public Method getMethod() {
        return this.myMethod;
    }


    protected InsidiousMethodReturnValueDescriptorImpl createDescriptorImpl(@NotNull Project project) {
        return new InsidiousMethodReturnValueDescriptorImpl(project, this.myMethod, this.myReturnValue);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InsidiousMethodReturnValueData that = (InsidiousMethodReturnValueData) o;

        if (!this.myMethod.equals(that.myMethod)) return false;
        return Objects.equals(this.myReturnValue, that.myReturnValue);
    }

    public int hashCode() {
        return Objects.hash(this.myReturnValue, this.myMethod);
    }


    public DisplayKey<InsidiousMethodReturnValueDescriptorImpl> getDisplayKey() {
        return new MethodReturnValueDisplayKey(this.myMethod, this.myReturnValue);
    }

    private static final class MethodReturnValueDisplayKey
            extends Pair<Method, Value> implements DisplayKey<InsidiousMethodReturnValueDescriptorImpl> {
        MethodReturnValueDisplayKey(@NotNull Method method, @Nullable Value value) {
            super(method, value);
        }
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\descriptor\InsidiousMethodReturnValueData.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */