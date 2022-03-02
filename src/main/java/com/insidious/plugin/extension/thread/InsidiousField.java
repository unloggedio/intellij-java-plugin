package com.insidious.plugin.extension.thread;

import com.insidious.plugin.extension.thread.types.InsidiousType;
import com.sun.jdi.*;
import org.jetbrains.annotations.NotNull;

public class InsidiousField implements Field {

    private final VirtualMachine virtualMachine;
    private final Type type;

    private InsidiousField(String typeName, VirtualMachine virtualMachine) {
        this.virtualMachine = virtualMachine;
        this.type = new InsidiousType(typeName, "L" + typeName, this.virtualMachine());
    }

    public static InsidiousField from(String typeName, VirtualMachine virtualMachine) {
        return new InsidiousField(typeName, virtualMachine);
    }

    public static InsidiousField from(java.lang.reflect.Field field, VirtualMachine virtualMachine) {
        return new InsidiousField(field.getType().getTypeName(), virtualMachine);
    }

    @Override
    public String typeName() {
        return type.name();
    }

    @Override
    public Type type() throws ClassNotLoadedException {
        return type;
    }

    @Override
    public boolean isTransient() {
        return false;
    }

    @Override
    public boolean isVolatile() {
        return false;
    }

    @Override
    public boolean isEnumConstant() {
        return false;
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public String signature() {
        return type.signature();
    }

    @Override
    public String genericSignature() {
        return type.signature();
    }

    @Override
    public ReferenceType declaringType() {
        return null;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public boolean isFinal() {
        return true;
    }

    @Override
    public boolean isSynthetic() {
        return false;
    }

    @Override
    public int modifiers() {
        return 0;
    }

    @Override
    public boolean isPrivate() {
        return false;
    }

    @Override
    public boolean isPackagePrivate() {
        return false;
    }

    @Override
    public boolean isProtected() {
        return false;
    }

    @Override
    public boolean isPublic() {
        return false;
    }

    @Override
    public VirtualMachine virtualMachine() {
        return this.virtualMachine;
    }

    @Override
    public int compareTo(@NotNull Field o) {
        return 0;
    }

}
