package extension.thread.types;

import com.sun.jdi.ModuleReference;
import com.sun.jdi.VirtualMachine;

public class InsidiousArrayTypeReference extends InsidiousReferenceType {
    @Override
    public ModuleReference module() {
        return super.module();
    }

    @Override
    public VirtualMachine virtualMachine() {
        return null;
    }
}
