package com.insidious.plugin.extension.thread.types;

import com.sun.jdi.*;

import java.util.List;
import java.util.Map;

public class InsidiousClassTypeReference extends InsidiousReferenceType {


    public InsidiousClassTypeReference(String name,
                                       String sourceName,
                                       String signature,
                                       Map<String, Field> fields,
                                       VirtualMachine virtualMachine) {
        super(name, signature, sourceName, fields, virtualMachine);
    }

    @Override
    public String signature() {
        return super.signature();
    }

    @Override
    public String name() {
        return super.name();
    }

    @Override
    public String sourceName() throws AbsentInformationException {
        return super.sourceName();
    }

    @Override
    public List<String> sourcePaths(String s) throws AbsentInformationException {
        return super.sourcePaths(s);
    }

    @Override
    public boolean isStatic() {
        return super.isStatic();
    }

    @Override
    public boolean isAbstract() {
        return super.isAbstract();
    }

    @Override
    public boolean isFinal() {
        return super.isFinal();
    }

    @Override
    public boolean isPrepared() {
        return super.isPrepared();
    }

    @Override
    public boolean isVerified() {
        return super.isVerified();
    }

    @Override
    public List<Field> fields() {
        return super.fields();
    }

    @Override
    public List<Field> visibleFields() {
        return super.visibleFields();
    }

    @Override
    public List<Field> allFields() {
        return super.allFields();
    }

    @Override
    public Field fieldByName(String s) {
        return super.fieldByName(s);
    }

    @Override
    public List<Method> methods() {
        return super.methods();
    }

    @Override
    public List<Method> visibleMethods() {
        return super.visibleMethods();
    }

    @Override
    public List<Method> allMethods() {
        return super.allMethods();
    }

    @Override
    public List<Method> methodsByName(String s) {
        return super.methodsByName(s);
    }

    @Override
    public List<Method> methodsByName(String s, String s1) {
        return super.methodsByName(s, s1);
    }

    @Override
    public Value getValue(Field field) {
        return super.getValue(field);
    }

    @Override
    public Map<Field, Value> getValues(List<? extends Field> list) {
        return super.getValues(list);
    }

    @Override
    public boolean isPrivate() {
        return super.isPrivate();
    }

    @Override
    public boolean isPackagePrivate() {
        return super.isPackagePrivate();
    }

    @Override
    public boolean isProtected() {
        return super.isProtected();
    }

    @Override
    public boolean isPublic() {
        return super.isPublic();
    }

    @Override
    public VirtualMachine virtualMachine() {
        return super.virtualMachine();
    }
}
