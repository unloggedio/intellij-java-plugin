package com.insidious.plugin.extension.thread.types;

import com.sun.jdi.*;
import com.insidious.plugin.extension.thread.InsidiousLocalVariable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InsidiousObjectReference implements ObjectReference {

    private final Map<String, InsidiousLocalVariable> valueMap;
    private final ThreadReference parentThread;
    private ReferenceType referenceType;
    private long objectId;

    public InsidiousObjectReference(ThreadReference parentThread) {
        this.valueMap = new HashMap<>();
        this.parentThread = parentThread;
    }

    @Override
    public String toString() {
        return "InsidiousObjectReference{type=" + referenceType.name() + ",value=" + valueMap + "}";
    }

    public void setReferenceType(ReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public void setObjectId(long objectId) {
        this.objectId = objectId;
    }

    @Override
    public ReferenceType referenceType() {
        return referenceType;
    }

    @Override
    public Value getValue(Field field) {
        return valueMap.get(field.name()).getValue();
    }

    @Override
    public Map<Field, Value> getValues(List<? extends Field> list) {
        Map<Field, Value> values = new HashMap<>();
        for (Field field : list) {
            values.put(field, valueMap.get(field.name()).getValue());
        }
        return values;
    }

    public Map<String, InsidiousLocalVariable> getValues() {
        return valueMap;
    }

    @Override
    public void setValue(Field field, Value value) throws InvalidTypeException, ClassNotLoadedException {

    }

    @Override
    public Value invokeMethod(ThreadReference threadReference, Method method, List<? extends Value> list, int i) throws InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException, InvocationException {
        return null;
    }

    @Override
    public void disableCollection() {

    }

    @Override
    public void enableCollection() {

    }

    @Override
    public boolean isCollected() {
        return false;
    }

    @Override
    public long uniqueID() {
        return objectId;
    }

    @Override
    public List<ThreadReference> waitingThreads() throws IncompatibleThreadStateException {
        return null;
    }

    @Override
    public ThreadReference owningThread() throws IncompatibleThreadStateException {
        return parentThread;
    }

    @Override
    public int entryCount() throws IncompatibleThreadStateException {
        return 0;
    }

    @Override
    public List<ObjectReference> referringObjects(long l) {
        return null;
    }

    @Override
    public Type type() {
        return referenceType;
    }

    @Override
    public VirtualMachine virtualMachine() {
        return referenceType.virtualMachine();
    }
}
