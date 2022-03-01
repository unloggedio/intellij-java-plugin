package com.insidious.plugin.extension.thread.types;

import com.sun.jdi.IntegerType;
import com.sun.jdi.VirtualMachine;

public class IntegerTypeImpl implements IntegerType {

    private final VirtualMachine virtualMachine;

    public IntegerTypeImpl(VirtualMachine virtualMachine) {
        this.virtualMachine = virtualMachine;
    }

    @Override
    public String signature() {
        return "insidious type signature";
    }

    @Override
    public String name() {
        return "insidious integer type";
    }

    @Override
    public VirtualMachine virtualMachine() {
        return virtualMachine;
    }
}
