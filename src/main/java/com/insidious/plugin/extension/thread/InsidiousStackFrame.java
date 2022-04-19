package com.insidious.plugin.extension.thread;

import com.insidious.plugin.extension.thread.types.InsidiousObjectReference;
import com.sun.jdi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class InsidiousStackFrame implements StackFrame {

    private final ThreadReference threadReference;
    private final VirtualMachine virtualMachine;
    private InsidiousObjectReference thisObject;
    private final List<LocalVariable> localVariables;
    private InsidiousLocation location;

    public InsidiousStackFrame(InsidiousLocation location,
                               ThreadReference threadReference,
                               InsidiousObjectReference thisObject,
                               VirtualMachine virtualMachine) {
        this.location = location;
        this.threadReference = threadReference;
        this.virtualMachine = virtualMachine;
        this.localVariables = new LinkedList<>();
        this.thisObject = thisObject;

    }

    @Override
    public Location location() {
        return location;
    }

    @Override
    public ThreadReference thread() {
        return threadReference;
    }

    @Nullable
    @Override
    public ObjectReference thisObject() {
        return thisObject;
    }

    @NotNull
    @Override
    public List<LocalVariable> visibleVariables() throws AbsentInformationException {
        return localVariables;
    }

    public List<LocalVariable> getLocalVariables() {
        return localVariables;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(InsidiousLocation currentLocation) {
        this.location = currentLocation;
    }

    @Nullable
    @Override
    public LocalVariable visibleVariableByName(String s) throws AbsentInformationException {
        return localVariables.stream().filter(e -> e.name().equals(s)).findFirst().get();
    }

    @Override
    public Value getValue(LocalVariable localVariable) {
        InsidiousLocalVariable variable = (InsidiousLocalVariable) localVariable;
        return variable.getValue();
    }

    @NotNull
    @Override
    public Map<LocalVariable, Value> getValues(List<? extends LocalVariable> list) {
        HashMap<LocalVariable, Value> valueMap = new HashMap<>();

        for (LocalVariable localVariable : list) {
            InsidiousLocalVariable insidiousVariable = (InsidiousLocalVariable) localVariable;
            valueMap.put(localVariables.get(0), insidiousVariable.getValue());
        }

        return valueMap;
    }

    @Override
    public void setValue(LocalVariable localVariable, Value value) throws InvalidTypeException, ClassNotLoadedException {
        new Exception().printStackTrace();
    }

    @NotNull
    @Override
    public List<Value> getArgumentValues() {
        return Arrays.asList();
    }

    @Override
    public VirtualMachine virtualMachine() {
        return virtualMachine;
    }

    public void setThisObject(InsidiousObjectReference thisObject) {
        this.thisObject = thisObject;
    }
}
