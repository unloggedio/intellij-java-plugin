package extension.connector;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.jdi.ThreadGroupReferenceProxy;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Comparing;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.ThreadReference;
import extension.InsidiousStackFrameProxyImpl;
import extension.InsidiousThreadReferenceProxy;
import extension.InsidiousVirtualMachineProxy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class InsidiousThreadReferenceProxyImpl implements InsidiousThreadReferenceProxy {
    public static final Comparator<InsidiousThreadReferenceProxy> ourComparator;
    private static final Logger logger = Logger.getInstance(InsidiousThreadReferenceProxyImpl.class);

    static {
        ourComparator = ((th1, th2) -> {
            int res = Comparing.compare(th2.isSuspended(), th1.isSuspended());
            return (res == 0) ? th1.getThreadReference().name().compareToIgnoreCase(th2.getThreadReference().name()) : res;
        });
    }

    ThreadReference threadReference;
    InsidiousVirtualMachineProxy virtualMachineProxy;
    ThreadGroupReferenceProxy threadGroupReferenceProxy;
    private Integer myFrameCount;


    public InsidiousThreadReferenceProxyImpl(InsidiousVirtualMachineProxy virtualMachineProxy, ThreadReference threadReference) {
        this.virtualMachineProxy = virtualMachineProxy;
        this.threadReference = threadReference;
        this.threadGroupReferenceProxy = new InsidiousThreadGroupReferenceProxyImpl(virtualMachineProxy, threadReference.threadGroup());
    }

    public InsidiousVirtualMachineProxy getVirtualMachine() {
        return this.virtualMachineProxy;
    }

    public ThreadReference getThreadReference() {
        return this.threadReference;
    }

    public InsidiousStackFrameProxy frame(int i) throws EvaluateException {
        try {
            InsidiousStackFrameProxy frameProxy = new InsidiousStackFrameProxyImpl(this, i);
            return frameProxy;
        } catch (IndexOutOfBoundsException e) {
            throw new EvaluateException(e.getMessage(), e);
        }
    }

    public List<InsidiousStackFrameProxy> frames() throws EvaluateException {
        List<InsidiousStackFrameProxy> frames = new ArrayList<>();
        for (int index = 0; index < frameCount(); index++) {
            InsidiousStackFrameProxy proxy = new InsidiousStackFrameProxyImpl(this, index);
            frames.add(proxy);
        }
        return frames;
    }

    public ThreadGroupReferenceProxy getThreadGroupProxy() {
        return this.threadGroupReferenceProxy;
    }

    public boolean isCollected() {
        return this.threadReference.isCollected();
    }

    public boolean isSuspended() {
        return this.threadReference.isSuspended();
    }

    public int status() {
        return this.threadReference.status();
    }

    public int frameCount() throws EvaluateException {
        try {
            if (this.myFrameCount == null) {
                this.myFrameCount = Integer.valueOf(this.threadReference.frameCount());
            }
        } catch (IncompatibleThreadStateException e) {
            logger.debug("IncompatibleThreadStateException while getting frameCount:", e);
            throw new EvaluateException(e.getMessage(), e);
        }
        return this.myFrameCount.intValue();
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\connector\InsidiousThreadReferenceProxyImpl.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */