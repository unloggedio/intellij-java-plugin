package extension.thread;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.jdi.VirtualMachineProxy;
import com.sun.jdi.*;
import extension.InsidiousLocalVariableProxy;
import extension.connector.InsidiousStackFrameProxy;
import extension.thread.types.IntegerTypeImpl;

import java.util.List;

public class InsidiousStackFrameProxyImpl implements InsidiousStackFrameProxy {
    private final InsidiousThreadReferenceProxy threadProxy;
    private final int frameIndex;
    private final InsidiousLocation location;
    private final InsidiousStackFrame stackFrame;

    public InsidiousStackFrameProxyImpl(InsidiousThreadReferenceProxy threadProxy, int frameIndex) {
        this.threadProxy = threadProxy;
        this.frameIndex = frameIndex;
        location = new InsidiousLocation(new InsidiousReferenceType(),
                "gcdOfTwoNumbers", 5,
                "org/zerhusen/service/GCDService",
                "org/zerhusen/service/GCDService.java", 20);
        stackFrame = new InsidiousStackFrame(location, threadProxy.getThreadReference(), null, threadProxy.getVirtualMachine().getVirtualMachine());
    }

    @Override
    public InsidiousThreadReferenceProxy threadProxy() {
        return threadProxy;
    }

    @Override
    public String getVariableName(InsidiousLocalVariableProxy insidiousLocalVariableProxy) throws EvaluateException {
        return "variable-1";
    }

    @Override
    public Value getValue(InsidiousLocalVariableProxy paramInsidiousLocalVariableProxy) throws EvaluateException {
        return new InsidiousValue(new IntegerTypeImpl(
                threadProxy.getVirtualMachine().getVirtualMachine()),
                threadProxy.getVirtualMachine().getVirtualMachine());
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
        return stackFrame;
//        return null;
    }

    @Override
    public int getFrameIndex() throws EvaluateException {
        return 0;
    }

    @Override
    public VirtualMachineProxy getVirtualMachine() {
        return threadProxy.getVirtualMachine();
    }

    @Override
    public Location location() throws EvaluateException {
        return location;
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
