package extension.thread;

import com.sun.jdi.*;
import org.jetbrains.annotations.NotNull;

public class InsidiousLocalVariable implements LocalVariable {

    private final String name;
    private final String typeName;
    private final String signature;
    private final VirtualMachine virtualMachine;

    public InsidiousLocalVariable(String name, String typeName, String signature, VirtualMachine virtualMachine) {
        this.name = name;
        this.typeName = typeName;
        this.signature = signature;
        this.virtualMachine = virtualMachine;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String typeName() {
        return typeName;
    }

    @Override
    public Type type() throws ClassNotLoadedException {
        return null;
    }

    @Override
    public String signature() {
        return signature;
    }

    @Override
    public String genericSignature() {
        return signature;
    }

    @Override
    public boolean isVisible(StackFrame stackFrame) {
        return true;
    }

    @Override
    public boolean isArgument() {
        return false;
    }

    @Override
    public VirtualMachine virtualMachine() {
        return virtualMachine;
    }

    @Override
    public int compareTo(@NotNull LocalVariable localVariable) {
        return 0;
    }
}
