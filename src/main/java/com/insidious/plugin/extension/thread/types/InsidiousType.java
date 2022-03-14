package com.insidious.plugin.extension.thread.types;

import com.sun.jdi.Type;
import com.sun.jdi.VirtualMachine;

public class InsidiousType implements Type {


    private final String name;
    private final String signature;
    private final VirtualMachine virtualMachine;
    private final String nameUnqualified;

    public InsidiousType(String name, String signature, VirtualMachine virtualMachine) {
        this.name = name;
        String[] parts = name.split("\\.");
        this.nameUnqualified = parts[parts.length - 1];
        this.signature = signature;
        this.virtualMachine = virtualMachine;
    }

    @Override
    public String signature() {
        return signature;
    }

    @Override
    public String name() {
        return nameUnqualified;
    }

    @Override
    public VirtualMachine virtualMachine() {
        return virtualMachine;
    }
}
