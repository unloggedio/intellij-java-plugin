package com.insidious.plugin.extension.thread;

import com.sun.jdi.*;
import org.jetbrains.annotations.NotNull;

public class InsidiousLocalVariable implements LocalVariable {

    private final String name;
    private final String typeName;
    private final String signature;
    private final VirtualMachine virtualMachine;
    private final InsidiousValue value;
    private final long id;

    @Override
    public String toString() {
        return value.toString();
    }

    public InsidiousLocalVariable(String name, String typeName, String signature, long objectId, InsidiousValue value, VirtualMachine virtualMachine) {
        this.name = name;
        this.typeName = typeName;
        this.id = objectId;
        this.signature = signature;
        this.virtualMachine = virtualMachine;
        this.value = value;
    }

    public InsidiousValue getValue() {
        return value;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String typeName() {
        return value.type().name();
    }

    @Override
    public Type type() throws ClassNotLoadedException {
        return value.type();
    }

    @Override
    public String signature() {
        return value.type().signature();
    }

    @Override
    public String genericSignature() {
        return value.type().signature();
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
