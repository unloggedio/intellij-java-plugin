package com.insidious.plugin.extension.connector;

import com.insidious.plugin.extension.InsidiousJavaDebugProcess;
import com.insidious.plugin.extension.InsidiousXSuspendContext;
import com.insidious.plugin.extension.thread.*;
import com.insidious.plugin.pojo.TracePoint;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.PositionManager;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.requests.LocatableEventRequestor;
import com.intellij.debugger.requests.ClassPrepareRequestor;
import com.intellij.debugger.requests.Requestor;
//import com.intellij.debugger.ui.breakpoints.Breakpoint;
//import com.intellij.debugger.ui.breakpoints.MethodBreakpoint;
//import com.intellij.debugger.ui.breakpoints.SteppingBreakpoint;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.Key;
import com.intellij.xdebugger.XDebugProcess;
import com.sun.jdi.*;
import com.sun.jdi.request.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class InsidiousJDIConnector implements InsidiousVirtualMachineProxy {
    public static final Requestor REQUESTOR = null;
    public static final Key<Requestor> REQUEST_HINT = Key.create("RequestHint");
    private final static Logger logger = LoggerUtil.getInstance(InsidiousJDIConnector.class);
    private final InsidiousJavaDebugProcess insidiousJavaDebugProcess;
    private final InsidiousVirtualMachine virtualMachine;

    public InsidiousJDIConnector(InsidiousJavaDebugProcess insidiousJavaDebugProcess) {
        this.virtualMachine = new InsidiousVirtualMachine(insidiousJavaDebugProcess.getProject());
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
        logger.error("method not implemented", new Exception());
        return null;
    }

    public EventRequestManager getEventRequestManager() {
        return this.virtualMachine.eventRequestManager();
    }


    public void resume() {
//        logger.error("method not implemented", new Exception());
    }

    public <K, V> ClassPrepareRequest createClassPrepareRequest(String classPattern, ClassPrepareRequestor requestor) {
        logger.warn("method not implemented " +
                "createClassPrepareRequest [" + classPattern + "] [" + requestor + "]- " + new Exception().getStackTrace()[0]);
        return new VideobugClassPrepareRequest(classPattern, requestor, this.virtualMachine);
    }

    public ReferenceType findClassesByName(String className) {
        logger.warn("method not implemented findClassesByName", new Exception());
        return null;
    }

    public void attachVirtualMachine(String hostName, String address, boolean useSockets, boolean serverMode, String timeout) {
//        logger.error("method not implemented", new Exception());
    }

    public void createThreadStartRequest() {
//        logger.error("method not implemented", new Exception());

    }

    public void deleteEventRequest(EventRequest request) {
        logger.error("method not implemented deleteEventRequest", new Exception());

    }

    public void doStep(InsidiousXSuspendContext suspendContext, int size, int depth, RequestHint requestHint) {

        virtualMachine.doStep(suspendContext.getThreadReferenceProxy(), size, depth, requestHint);
        insidiousJavaDebugProcess.notifySuspended();


    }

    public void disableAllBreakpoints() {
        logger.error("method not implemented disableAllBreakpoints", new Exception());

    }

    public void createLocationBreakpoint(Location myRunToCursorLocation, int suspendPolicy, LocatableEventRequestor myRunToCursorBreakpoint) {
        logger.error("method not implemented createLocationBreakpoint", new Exception());
    }

    public Iterable<? extends BreakpointRequest> getBreakpointsWithRequestor(Requestor runToCursorBreakpoint) {
        logger.warn("method not implemented getBreakpointsWithRequestor - " + new Exception().getStackTrace()[0].getFileName());
        return null;
    }

    public void enableAllBreakpoints() {
        logger.warn("method not implemented enableAllBreakpoints- " + new Exception().getStackTrace()[0].getFileName());

    }

//    public void createFieldWatchpoint(ReferenceType declaringType, String name, Breakpoint myJumpToAssignmentBreakpoint) {
//        logger.warn("method not implemented createFieldWatchpoint- " + new Exception().getStackTrace()[0].getFileName());
//
//    }

    public Iterable<? extends ModificationWatchpointRequest> getModificationWatchpointsWithRequestor(Requestor jumpToAssignmentBreakpoint) {
        logger.warn("method not implemented getModificationWatchpointsWithRequestor- " + new Exception().getStackTrace()[0].getFileName());
        return null;
    }

    public void doStep(InsidiousXSuspendContext context, int size, int depth) {
        doStep(context, size, depth, null);
    }

    public void dispose() {
        virtualMachine.dispose();
        logger.warn("method not implemented - connector.dispose");
    }

    public List<BreakpointRequest> getAllBreakpoints() {
        logger.warn("method not implemented - connector.getAllBreakpoints");
        return Collections.emptyList();
    }

//    public void createExceptionBreakpoint(ReferenceType referenceType, boolean notifyCaught,
//                                          boolean notifyUncaught, int i, Breakpoint breakpoint) {
//        logger.warn("method not implemented createExceptionBreakpoint- " + new Exception().getStackTrace()[0].getFileName());
//
//    }

    public List<ExceptionRequest> getAllExceptionBreakpoints() {
        logger.warn("method not implemented getAllExceptionBreakpoints- " + new Exception().getStackTrace()[0].getFileName());
        return null;
    }

//    public void createMethodBreakpoint(ReferenceType referenceType, MethodBreakpoint bp) {
//        logger.warn("method not implemented createMethodBreakpoint- " + new Exception().getStackTrace()[0].getFileName());
//    }

    public List<EventRequest> getAllMethodRequests() {
        logger.warn("method not implemented getAllMethodRequests- " + new Exception().getStackTrace()[0].getFileName());
        return null;
    }

    public List<ModificationWatchpointRequest> getAllFieldWatchpoints() {
        logger.warn("method not implemented getAllFieldWatchpoints- " + new Exception().getStackTrace()[0].getFileName());
        return null;
    }

//    public void createSteppingBreakpoint(InsidiousXSuspendContext context, SteppingBreakpoint breakpoint, RequestHint hint) {
//        logger.warn("method not implemented createSteppingBreakpoint- " + new Exception().getStackTrace()[0].getFileName());
//
//    }

    public void setTracePoint(TracePoint tracePoint, ProgressIndicator indicator) throws Exception {
        this.virtualMachine.setTracePoint(tracePoint);
    }
}
