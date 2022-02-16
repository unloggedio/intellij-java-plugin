package extension.connector;

import com.intellij.debugger.PositionManager;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.requests.Requestor;
import com.intellij.xdebugger.XDebugProcess;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import extension.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class InsidiousConnector implements InsidiousVirtualMachineProxy {
    public static final Requestor REQUESTOR = null;
    private final InsidiousDebugProcess insidiousDebugProcess;

    public InsidiousConnector(InsidiousDebugProcess insidiousDebugProcess) {

        this.insidiousDebugProcess = insidiousDebugProcess;
    }

    public ThreadReference getThreadReferenceWithUniqueId(int uniqueID) {
        return new InsidiousThreadReference();
    }

    @Override
    public @NotNull VirtualMachine getVirtualMachine() {
        return null;
    }

    @Override
    public @NotNull XDebugProcess getXDebugProcess() {
        return null;
    }

    @Override
    public @NotNull PositionManager getPositionManager() {
        return null;
    }

    @Override
    public boolean isAttached() {
        return false;
    }

    @Override
    public boolean canBeModified() {
        return false;
    }

    @Override
    public boolean canGetSyntheticAttribute() {
        return false;
    }

    @Override
    public boolean isCollected(ObjectReference paramObjectReference) {
        return false;
    }

    @Override
    public List<InsidiousThreadGroupReferenceProxy> topLevelThreadGroups() {
        return null;
    }

    @Override
    public List<InsidiousThreadReferenceProxy> allThreads() {
        return null;
    }

    @Override
    public List<ReferenceType> allClasses() {
        return null;
    }

    @Override
    public boolean canGetBytecodes() {
        return false;
    }

    @Override
    public boolean versionHigher(String version) {
        return false;
    }

    @Override
    public boolean canWatchFieldModification() {
        return false;
    }

    @Override
    public boolean canWatchFieldAccess() {
        return false;
    }

    @Override
    public boolean canInvokeMethods() {
        return false;
    }

    @Override
    public DebugProcess getDebugProcess() {
        return null;
    }

    @Override
    public List<ReferenceType> nestedTypes(ReferenceType refType) {
        return null;
    }

    @Override
    public List<ReferenceType> classesByName(@NotNull String s) {
        return null;
    }

    public Object createString(String join) {
        new Exception().printStackTrace();
        return join;
    }
}
