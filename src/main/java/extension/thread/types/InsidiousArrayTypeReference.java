package extension.thread.types;

import com.sun.jdi.ModuleReference;
import com.sun.jdi.VirtualMachine;

public class InsidiousArrayTypeReference extends InsidiousReferenceType {
    public InsidiousArrayTypeReference(String name, String signature, VirtualMachine virtualMachine) {
        super(name, signature, null, null, virtualMachine);
    }

    @Override
    public ModuleReference module() {
        return super.module();
    }

    @Override
    public VirtualMachine virtualMachine() {
        return super.virtualMachine();
    }
}
