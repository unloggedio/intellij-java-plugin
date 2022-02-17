package extension.connector;

import com.intellij.debugger.PositionManager;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.requests.LocatableEventRequestor;
import com.intellij.debugger.requests.ClassPrepareRequestor;
import com.intellij.debugger.requests.Requestor;
import com.intellij.debugger.ui.breakpoints.Breakpoint;
import com.intellij.debugger.ui.breakpoints.MethodBreakpoint;
import com.intellij.debugger.ui.breakpoints.SteppingBreakpoint;
import com.intellij.openapi.util.Key;
import com.intellij.xdebugger.XDebugProcess;
import com.sun.jdi.*;
import com.sun.jdi.request.*;
import extension.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class InsidiousJDIConnector implements InsidiousVirtualMachineProxy {
    public static final Requestor REQUESTOR = null;
    public static final Key<Requestor> REQUEST_HINT = Key.create("RequestHint");
    private final InsidiousJavaDebugProcess insidiousJavaDebugProcess;
    private final VirtualMachine virtualMachine = null;

    public InsidiousJDIConnector(InsidiousJavaDebugProcess insidiousJavaDebugProcess) {
//        this.virtualMachine = new VirtualM
//        new EventRequestManager();
        this.insidiousJavaDebugProcess = insidiousJavaDebugProcess;
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
        return insidiousJavaDebugProcess;
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
        return insidiousJavaDebugProcess.getConnector().getDebugProcess();
    }

    @Override
    public List<ReferenceType> nestedTypes(ReferenceType refType) {
        return null;
    }

    @Override
    public List<ReferenceType> classesByName(@NotNull String s) {
        return null;
    }

    public StringReference createString(String join) {
        new Exception().printStackTrace();
        return null;
    }

    public EventRequestManager getEventRequestManager() {
        return this.virtualMachine.eventRequestManager();
    }


    public void resume() {
        new Exception().printStackTrace();
    }

    public ClassPrepareRequest createClassPrepareRequest(String classPattern, ClassPrepareRequestor requestor) {
        new Exception().printStackTrace();
        return null;
    }

    public ReferenceType findClassesByName(String className) {
        new Exception().printStackTrace();
        return null;
    }

    public void attachVirtualMachine(String hostName, String address, boolean useSockets, boolean serverMode, String timeout) {
        new Exception().printStackTrace();
    }

    public void createThreadStartRequest() {
        new Exception().printStackTrace();

    }

    public void deleteEventRequest(EventRequest request) {
        new Exception().printStackTrace();

    }

    public void doStep(InsidiousXSuspendContext suspendContext, int i, int nextStepDepth, RequestHint hint) {
        new Exception().printStackTrace();

    }

    public void disableAllBreakpoints() {
        new Exception().printStackTrace();

    }

    public void createLocationBreakpoint(Location myRunToCursorLocation, int suspendPolicy, LocatableEventRequestor myRunToCursorBreakpoint) {
        new Exception().printStackTrace();
    }

    public Iterable<? extends BreakpointRequest> getBreakpointsWithRequestor(Requestor runToCursorBreakpoint) {
        new Exception().printStackTrace();
        return null;
    }

    public void enableAllBreakpoints() {
        new Exception().printStackTrace();

    }

    public void createFieldWatchpoint(ReferenceType declaringType, String name, Breakpoint myJumpToAssignmentBreakpoint) {
        new Exception().printStackTrace();

    }

    public Iterable<? extends ModificationWatchpointRequest> getModificationWatchpointsWithRequestor(Requestor jumpToAssignmentBreakpoint) {
        new Exception().printStackTrace();
        return null;
    }

    public void doStep(InsidiousXSuspendContext context, int i, int i1) {
        new Exception().printStackTrace();
    }

    public void dispose() {
        new Exception().printStackTrace();

    }

    public List<BreakpointRequest> getAllBreakpoints() {
        new Exception().printStackTrace();
        return null;
    }

    public void createExceptionBreakpoint(ReferenceType referenceType, boolean notifyCaught, boolean notifyUncaught, int i, Breakpoint breakpoint) {
        new Exception().printStackTrace();

    }

    public List<ExceptionRequest> getAllExceptionBreakpoints() {
        new Exception().printStackTrace();
        return null;
    }

    public void createMethodBreakpoint(ReferenceType referenceType, MethodBreakpoint bp) {
        new Exception().printStackTrace();

    }

    public List<EventRequest> getAllMethodRequests() {
        new Exception().printStackTrace();
        return null;
    }

    public List<ModificationWatchpointRequest> getAllFieldWatchpoints() {
        new Exception().printStackTrace();
        return null;
    }

    public void createSteppingBreakpoint(InsidiousXSuspendContext context, SteppingBreakpoint breakpoint, RequestHint hint) {
        new Exception().printStackTrace();

    }
}
