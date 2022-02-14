package extension;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.jdi.VirtualMachineProxy;
import com.sun.jdi.*;

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
    public String getVariableName(InsidiousLocalVariableProxy paramUndoLocalVariableProxy) throws EvaluateException {
        return null;
    }

    @Override
    public Value getValue(InsidiousLocalVariableProxy paramUndoLocalVariableProxy) throws EvaluateException {
        return null;
    }

    @Override
    public void setValue(InsidiousLocalVariableProxy paramUndoLocalVariableProxy, Value paramValue) throws ClassNotLoadedException, InvalidTypeException, EvaluateException {

    }

    @Override
    public Type getType(InsidiousLocalVariableProxy paramUndoLocalVariableProxy) throws EvaluateException, ClassNotLoadedException {
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
