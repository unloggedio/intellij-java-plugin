package com.insidious.plugin.extension.thread;

import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

public class InsidiousValue implements Value {

    private final Type type;
    private final VirtualMachine virtualMachine;
    private final Object actualValue;

    public InsidiousValue(Type type, Object value, VirtualMachine virtualMachine) {
        this.type = type;
        this.actualValue = value;
        this.virtualMachine = virtualMachine;
    }

    public Object getActualValue() {
        return actualValue;
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
