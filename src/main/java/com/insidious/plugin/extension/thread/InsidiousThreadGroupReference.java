package com.insidious.plugin.extension.thread;

import com.sun.jdi.*;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.pojo.TracePoint;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class InsidiousThreadGroupReference implements ThreadGroupReference {

    private final VirtualMachine virtualMachine;
    private final ReplayData replayData;
    private final List<ThreadReference> threads;

    public InsidiousThreadGroupReference(VirtualMachine virtualMachine, ReplayData replayData, TracePoint tracePoint) {
        this.virtualMachine = virtualMachine;
        this.replayData = replayData;
        this.threads = Arrays.asList(
                new InsidiousThreadReference(this, replayData, tracePoint)
        );
    }


    @Override
    public String name() {
        return "Insidious thread group";
    }

    @Override
    public ThreadGroupReference parent() {
        return null;
    }

    @Override
    public void suspend() {
        new Exception().printStackTrace();
    }

    @Override
    public void resume() {
        new Exception().printStackTrace();

    }

    @Override
    public List<ThreadReference> threads() {
        return threads;
    }

    @Override
    public List<ThreadGroupReference> threadGroups() {
        return List.of(this);
    }

    @Override
    public ReferenceType referenceType() {
        return null;
    }

    @Override
    public Value getValue(Field field) {
        return null;
    }

    @Override
    public Map<Field, Value> getValues(List<? extends Field> list) {
        return null;
    }

    @Override
    public void setValue(Field field, Value value) throws InvalidTypeException, ClassNotLoadedException {
        new Exception().printStackTrace();

    }

    @Override
    public Value invokeMethod(ThreadReference threadReference, Method method, List<? extends Value> list, int i)
            throws InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException, InvocationException {
        return null;
    }

    @Override
    public void disableCollection() {
        new Exception().printStackTrace();

    }

    @Override
    public void enableCollection() {
        new Exception().printStackTrace();

    }

    @Override
    public boolean isCollected() {
        return false;
    }

    @Override
    public long uniqueID() {
        return 0;
    }

    @Override
    public List<ThreadReference> waitingThreads() throws IncompatibleThreadStateException {
        return null;
    }

    @Override
    public ThreadReference owningThread() throws IncompatibleThreadStateException {
        return null;
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
        return null;
    }

    @Override
    public VirtualMachine virtualMachine() {
        return virtualMachine;
    }
}
