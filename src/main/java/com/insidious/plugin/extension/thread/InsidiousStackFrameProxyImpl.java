package com.insidious.plugin.extension.thread;

import com.insidious.plugin.extension.connector.InsidiousStackFrameProxy;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.jdi.VirtualMachineProxy;
import com.sun.jdi.*;

import java.util.List;

public class InsidiousStackFrameProxyImpl implements InsidiousStackFrameProxy {
    private final InsidiousThreadReferenceProxy threadProxy;
    private final int frameIndex;
    private StackFrame stackFrame;
    private Location location;

    public InsidiousStackFrameProxyImpl(InsidiousThreadReferenceProxy threadProxy, InsidiousStackFrame stackFrame, int frameIndex) {
        this.threadProxy = threadProxy;
        this.frameIndex = frameIndex;
        this.stackFrame = stackFrame;
//        location = new InsidiousLocation(new InsidiousClassTypeReference(threadProxy.getVirtualMachine().getVirtualMachine()),
//                "gcdOfTwoNumbers", 5,
//                "org/zerhusen/service/GCDService",
//                "org/zerhusen/service/GCDService.java", 20);
//        stackFrame = new InsidiousStackFrame(location, threadProxy.getThreadReference(), null, threadProxy.getVirtualMachine().getVirtualMachine());
    }

    @Override
    public InsidiousThreadReferenceProxy threadProxy() {
        return threadProxy;
    }

    @Override
    public String getVariableName(InsidiousLocalVariableProxy insidiousLocalVariableProxy) throws EvaluateException {
        return insidiousLocalVariableProxy.name();
    }


    @Override
    public Value getValue(InsidiousLocalVariableProxy paramInsidiousLocalVariableProxy) throws EvaluateException {
        return paramInsidiousLocalVariableProxy.getLocalVariable().getValue();
    }

    @Override
    public void setValue(InsidiousLocalVariableProxy paramInsidiousLocalVariableProxy, Value paramValue) throws ClassNotLoadedException, InvalidTypeException, EvaluateException {
        new Exception().printStackTrace();
    }

    @Override
    public Type getType(InsidiousLocalVariableProxy paramInsidiousLocalVariableProxy) throws EvaluateException, ClassNotLoadedException {
        return paramInsidiousLocalVariableProxy.getType();
    }

    @Override
    public StackFrame getStackFrame() throws EvaluateException {

        try {
            if (this.stackFrame == null) {
                this.stackFrame = this.threadProxy.getThreadReference().frame(getFrameIndex());
            } else {

                try {
                    this.stackFrame.thread();
                } catch (InvalidStackFrameException ex) {
                    this.stackFrame = this.threadProxy.getThreadReference().frame(getFrameIndex());
                }
            }
        } catch (Exception e) {
            this.stackFrame = null;
            throw new EvaluateException(e.getMessage(), e);
        }
        return this.stackFrame;
    }

    @Override
    public int getFrameIndex() throws EvaluateException {
        return frameIndex;
    }

    @Override
    public VirtualMachineProxy getVirtualMachine() {
        return threadProxy.getVirtualMachine();
    }

    @Override
    public Location location() throws EvaluateException {
        if (this.location == null) {
            for (int attempt = 0; attempt < 2; attempt++) {
                try {
                    this.location = getStackFrame().location();
                    break;
                } catch (InvalidStackFrameException ex) {
                    if (attempt != 0) {
                        throw new EvaluateException(ex.getMessage(), ex);
                    }
                }
            }
        }
        return this.location;
    }

    @Override
    public ClassLoaderReference getClassLoader() throws EvaluateException {
        return null;
    }

    @Override
    public InsidiousLocalVariableProxy visibleVariableByName(String paramString) throws EvaluateException {
        return null;
    }

    @Override
    public List<InsidiousLocalVariableProxy> visibleVariables() {
        return null;
    }

    @Override
    public List<Value> getArgumentValues() throws EvaluateException {
        return null;
    }

    @Override
    public ObjectReference thisObject() throws EvaluateException {
        return stackFrame.thisObject();
    }
}
