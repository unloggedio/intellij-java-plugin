package extension;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.jdi.ThreadGroupReferenceProxy;
import com.sun.jdi.ThreadReference;

import java.util.List;

public class InsidiousThreadReferenceProxyImpl implements InsidiousThreadReferenceProxy {
    private final InsidiousVirtualMachineProxy connector;
    private final ThreadReference newThread;

    public InsidiousThreadReferenceProxyImpl(InsidiousVirtualMachineProxy connector, ThreadReference newThread) {

        this.connector = connector;
        this.newThread = newThread;
    }

    @Override
    public InsidiousVirtualMachineProxy getVirtualMachine() {
        return null;
    }

    @Override
    public ThreadReference getThreadReference() {
        return null;
    }

    @Override
    public InsidiousStackFrameProxy frame(int paramInt) throws EvaluateException {
        return null;
    }

    @Override
    public int frameCount() throws EvaluateException {
        return 0;
    }

    @Override
    public List<InsidiousStackFrameProxy> frames() throws EvaluateException {
        return null;
    }

    @Override
    public ThreadGroupReferenceProxy getThreadGroupProxy() {
        return null;
    }

    @Override
    public boolean isCollected() {
        return false;
    }

    @Override
    public boolean isSuspended() {
        return false;
    }

    @Override
    public int status() {
        return 0;
    }
}
