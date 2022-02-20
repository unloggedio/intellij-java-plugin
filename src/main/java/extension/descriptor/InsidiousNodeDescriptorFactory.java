package extension.descriptor;

import com.intellij.debugger.impl.descriptors.data.DescriptorData;
import com.intellij.debugger.ui.tree.ArrayElementDescriptor;
import com.intellij.debugger.ui.tree.FieldDescriptor;
import com.intellij.debugger.ui.tree.NodeDescriptor;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import extension.thread.InsidiousLocalVariableProxy;
import org.jetbrains.annotations.NotNull;

public interface InsidiousNodeDescriptorFactory {
    ArrayElementDescriptor getArrayItemDescriptor(NodeDescriptor paramNodeDescriptor, ArrayReference paramArrayReference, int paramInt);

    @NotNull
    FieldDescriptor getFieldDescriptor(NodeDescriptor paramNodeDescriptor, ObjectReference paramObjectReference, Field paramField);

    InsidiousLocalVariableDescriptor getLocalVariableDescriptor(NodeDescriptor paramNodeDescriptor, InsidiousLocalVariableProxy paramInsidiousLocalVariableProxy);

    InsidiousUserExpressionDescriptor getUserExpressionDescriptor(NodeDescriptor paramNodeDescriptor, DescriptorData<InsidiousUserExpressionDescriptor> paramDescriptorData);
}

