package extension.thread;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.jdi.VirtualMachineProxy;
import com.jetbrains.jdi.StackFrameImpl;
import com.sun.jdi.*;
import extension.InsidiousLocalVariableProxy;
import extension.connector.InsidiousStackFrameProxy;
import extension.thread.InsidiousThreadReferenceProxy;

import java.util.List;

public class InsidiousStackFrameProxyImpl implements InsidiousStackFrameProxy {
    private final InsidiousThreadReferenceProxy threadProxy;
    private final int frameIndex;

    public InsidiousStackFrameProxyImpl(InsidiousThreadReferenceProxy threadProxy, int frameIndex) {
        this.threadProxy = threadProxy;
        this.frameIndex = frameIndex;
    }

    @Override
    public InsidiousThreadReferenceProxy threadProxy() {
        return null;
    }

    @Override
    public String getVariableName(InsidiousLocalVariableProxy paramInsidiousLocalVariableProxy) throws EvaluateException {
        return null;
    }

    @Override
    public Value getValue(InsidiousLocalVariableProxy paramInsidiousLocalVariableProxy) throws EvaluateException {
        return null;
    }

    @Override
    public void setValue(InsidiousLocalVariableProxy paramInsidiousLocalVariableProxy, Value paramValue) throws ClassNotLoadedException, InvalidTypeException, EvaluateException {

    }

    @Override
    public Type getType(InsidiousLocalVariableProxy paramInsidiousLocalVariableProxy) throws EvaluateException, ClassNotLoadedException {
        return null;
    }

    @Override
    public StackFrame getStackFrame() throws EvaluateException {
        return null;
    }

    @Override
    public int getFrameIndex() throws EvaluateException {
        return 0;
    }

    @Override
    public VirtualMachineProxy getVirtualMachine() {
        return null;
    }

    @Override
    public Location location() throws EvaluateException {
        return null;
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
        return null;
    }
}
