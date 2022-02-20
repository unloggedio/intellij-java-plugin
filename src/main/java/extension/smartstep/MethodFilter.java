package extension.smartstep;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.util.Range;
import com.sun.jdi.Location;
import extension.thread.InsidiousVirtualMachineProxy;
import extension.InsidiousXSuspendContext;
import extension.connector.InsidiousStackFrameProxy;
import extension.connector.RequestHint;
import org.jetbrains.annotations.Nullable;


public interface MethodFilter {
    boolean locationMatches(InsidiousVirtualMachineProxy paramInsidiousVirtualMachineProxy, Location paramLocation) throws EvaluateException;

    default boolean locationMatches(InsidiousVirtualMachineProxy virtualMachineProxy, Location location, @Nullable InsidiousStackFrameProxy frameProxy) throws EvaluateException {
        return locationMatches(virtualMachineProxy, location);
    }

    @Nullable
    Range<Integer> getCallingExpressionLines();

    default int onReached(InsidiousXSuspendContext context, RequestHint hint) {
        return 0;
    }

    default int getSkipCount() {
        return 0;
    }
}


