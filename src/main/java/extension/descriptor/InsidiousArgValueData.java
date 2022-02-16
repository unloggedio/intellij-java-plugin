package extension.descriptor;

import com.intellij.debugger.impl.descriptors.data.DescriptorData;
import com.intellij.debugger.impl.descriptors.data.DisplayKey;
import com.intellij.debugger.impl.descriptors.data.SimpleDisplayKey;
import com.intellij.openapi.project.Project;
import com.sun.jdi.Value;
import extension.connector.DecompiledLocalVariable;
import org.jetbrains.annotations.NotNull;

public class InsidiousArgValueData extends DescriptorData<InsidiousArgumentValueDescriptorImpl> {
    private final DecompiledLocalVariable myVariable;
    private final Value myValue;

    public InsidiousArgValueData(DecompiledLocalVariable variable, Value value) {
        this.myVariable = variable;
        this.myValue = value;
    }

    protected InsidiousArgumentValueDescriptorImpl createDescriptorImpl(@NotNull Project project) {
        return new InsidiousArgumentValueDescriptorImpl(project, this.myVariable, this.myValue);
    }

    public boolean equals(Object object) {
        if (!(object instanceof InsidiousArgValueData)) return false;

        return (this.myVariable.getSlot() == ((InsidiousArgValueData) object).myVariable.getSlot());
    }

    public int hashCode() {
        return this.myVariable.getSlot();
    }


    public DisplayKey<InsidiousArgumentValueDescriptorImpl> getDisplayKey() {
        return (DisplayKey<InsidiousArgumentValueDescriptorImpl>) new SimpleDisplayKey(Integer.valueOf(this.myVariable.getSlot()));
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\descriptor\InsidiousArgValueData.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */