package com.insidious.plugin.extension.connector;

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
import com.insidious.plugin.extension.InsidiousJavaDebugProcess;
import com.insidious.plugin.extension.InsidiousXSuspendContext;
import com.insidious.plugin.extension.model.DirectionType;
import com.insidious.plugin.extension.thread.*;
import com.insidious.plugin.network.Client;
import com.insidious.plugin.network.pojo.ExecutionSession;
import com.insidious.plugin.network.pojo.exceptions.APICallException;
import org.jetbrains.annotations.NotNull;
import com.insidious.plugin.pojo.TracePoint;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class InsidiousJDIConnector implements InsidiousVirtualMachineProxy {
    public static final Requestor REQUESTOR = null;
    public static final Key<Requestor> REQUEST_HINT = Key.create("RequestHint");
    private final InsidiousJavaDebugProcess insidiousJavaDebugProcess;
    private final InsidiousVirtualMachine virtualMachine;

    public InsidiousJDIConnector(InsidiousJavaDebugProcess insidiousJavaDebugProcess,
                                 Client client,
                                 ExecutionSession executionSession) throws APICallException, IOException {
        this.virtualMachine = new InsidiousVirtualMachine(client, executionSession);
        this.insidiousJavaDebugProcess = insidiousJavaDebugProcess;
    }

    public InsidiousThreadReference getThreadReferenceWithUniqueId(int uniqueID) {
        return (InsidiousThreadReference) this.virtualMachine.allThreads().get(0);
    }

    @Override
    public @NotNull VirtualMachine getVirtualMachine() {
        return virtualMachine;
    }

    @Override
    public @NotNull XDebugProcess getXDebugProcess() {
        return insidiousJavaDebugProcess;
    }

    @Override
    public @NotNull PositionManager getPositionManager() {
        return insidiousJavaDebugProcess.getPositionManager();
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
        return this.virtualMachine.allThreads().stream().map(
                        e -> new InsidiousThreadReferenceProxyImpl(this, (InsidiousThreadReference) e))
                .collect(Collectors.toList());
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
        throw new RuntimeException("use getXDebugProcess() instead");
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
//        new Exception().printStackTrace();
        return null;
    }

    public ReferenceType findClassesByName(String className) {
        new Exception().printStackTrace();
        return null;
    }

    public void attachVirtualMachine(String hostName, String address, boolean useSockets, boolean serverMode, String timeout) {
//        new Exception().printStackTrace();
    }

    public void createThreadStartRequest() {
//        new Exception().printStackTrace();

    }

    public void deleteEventRequest(EventRequest request) {
        new Exception().printStackTrace();

    }

    public void doStep(InsidiousXSuspendContext suspendContext, int size, int depth, RequestHint requestHint) {

        virtualMachine.doStep(suspendContext.getThreadReferenceProxy(), size, depth, requestHint);
        insidiousJavaDebugProcess.notifySuspended();


//        ThreadReference stepThread = suspendContext.getThreadReferenceProxy().getThreadReference();
//        if (stepThread == null) {
//            return;
//        }
//        try {
////            deleteStepRequests(stepThread);
//            EventRequestManager requestManager = this.virtualMachine.eventRequestManager();
//            StepRequest stepRequest = requestManager.createStepRequest(stepThread, size, depth);
//            stepRequest.setSuspendPolicy((suspendContext.getSuspendPolicy() == 1) ? 1 : 2);
//
//            if (requestHint != null) {
//                stepRequest.putProperty(REQUEST_HINT, requestHint);
//            }
//            try {
//                stepRequest.enable();
//            } catch (IllegalThreadStateException e) {
//                requestManager.deleteEventRequest(stepRequest);
//            }
//        } catch (ObjectCollectedException objectCollectedException) {
//        }
//
//        resume();


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

    public void doStep(InsidiousXSuspendContext context, int size, int depth) {
        doStep(context, size, depth, null);
    }

    public void dispose() {
        new Exception().printStackTrace();

    }

    public List<BreakpointRequest> getAllBreakpoints() {
        new Exception().printStackTrace();
        return Collections.emptyList();
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

    public void setTracePoint(TracePoint tracePoint, DirectionType direction) throws Exception {
        this.virtualMachine.setTracePoint(tracePoint, direction);

    }
}
