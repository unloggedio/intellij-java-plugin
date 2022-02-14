package extension;

import com.intellij.debugger.PositionManager;
import com.intellij.debugger.engine.jdi.VirtualMachineProxy;
import com.intellij.xdebugger.XDebugProcess;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.VirtualMachine;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface InsidiousVirtualMachineProxy extends VirtualMachineProxy {

    @NotNull
    VirtualMachine getVirtualMachine();

    @NotNull
    XDebugProcess getXDebugProcess();

    @NotNull
    PositionManager getPositionManager();

    boolean isAttached();

    boolean canBeModified();

    boolean canGetSyntheticAttribute();

    boolean isCollected(ObjectReference paramObjectReference);

    List<InsidiousThreadGroupReferenceProxy> topLevelThreadGroups();

    List<InsidiousThreadReferenceProxy> allThreads();
}
