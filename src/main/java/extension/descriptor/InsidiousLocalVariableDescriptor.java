package extension.descriptor;

import com.intellij.debugger.engine.jdi.LocalVariableProxy;

public interface InsidiousLocalVariableDescriptor extends InsidiousValueDescriptor {
    LocalVariableProxy getLocalVariable();
}