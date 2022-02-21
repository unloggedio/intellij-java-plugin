package extension.thread;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.jdi.ThreadGroupReferenceProxy;
import com.intellij.debugger.engine.jdi.ThreadReferenceProxy;
import extension.connector.InsidiousStackFrameProxy;

import java.util.List;

public interface InsidiousThreadReferenceProxy extends ThreadReferenceProxy {
    InsidiousVirtualMachineProxy getVirtualMachine();

    InsidiousStackFrameProxy frame(int paramInt) throws EvaluateException;

    List<InsidiousStackFrameProxy> frames() throws EvaluateException;

    ThreadGroupReferenceProxy getThreadGroupProxy();

    boolean isCollected();

    boolean isSuspended();

    InsidiousThreadReference getThreadReference();

    int status();
}
