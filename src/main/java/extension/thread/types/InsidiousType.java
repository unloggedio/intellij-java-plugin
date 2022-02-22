package extension.thread.types;

import com.sun.jdi.Type;
import com.sun.jdi.VirtualMachine;

public class InsidiousType implements Type {


    private final String name;
    private final String signature;
    private final VirtualMachine virtualMachine;

    public InsidiousType(String name, String signature, VirtualMachine virtualMachine) {
        this.name = name;
        this.signature = signature;
        this.virtualMachine = virtualMachine;
    }

    @Override
    public String signature() {
        return signature;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public VirtualMachine virtualMachine() {
        return virtualMachine;
    }
}
