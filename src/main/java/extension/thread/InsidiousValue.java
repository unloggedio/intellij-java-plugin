package extension.thread;

import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

public class InsidiousValue implements Value {

    private Type type;
    private VirtualMachine virtualMachine;

    public InsidiousValue(Type type, VirtualMachine virtualMachine) {
        this.type = type;
        this.virtualMachine = virtualMachine;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public VirtualMachine virtualMachine() {
        return virtualMachine;
    }
}
