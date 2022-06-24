package com.insidious.plugin.extension.thread;

import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.plugin.extension.connector.RequestHint;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.pojo.TracePoint;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.Project;
import com.sun.jdi.*;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.request.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class InsidiousVirtualMachine implements VirtualMachine {

    private static final Logger logger = LoggerUtil.getInstance(InsidiousVirtualMachine.class);
    private InsidiousThreadGroupReference threadReferenceGroup;
    private ReplayData replayData;
    private final Project project;

    public InsidiousVirtualMachine(Project project) {
        this.project = project;
    }


    @Override
    public List<ReferenceType> classesByName(String s) {
        return null;
    }

    @Override
    public List<ReferenceType> allClasses() {
        return null;
    }

    @Override
    public void redefineClasses(Map<? extends ReferenceType, byte[]> map) {
        new Exception().printStackTrace();
    }

    @Override
    public List<ThreadReference> allThreads() {
        return threadReferenceGroup.threads();
    }

    @Override
    public void suspend() {
        new Exception().printStackTrace();

    }

    @Override
    public void resume() {
        new Exception().printStackTrace();

    }

    @Override
    public List<ThreadGroupReference> topLevelThreadGroups() {
        return Arrays.asList(threadReferenceGroup);
    }

    @Override
    public EventQueue eventQueue() {
        return new EventQueue() {
            @Override
            public EventSet remove() throws InterruptedException {
                return null;
            }

            @Override
            public EventSet remove(long timeout) throws InterruptedException {
                return null;
            }

            @Override
            public VirtualMachine virtualMachine() {
                return null;
            }
        };
    }

    @Override
    public EventRequestManager eventRequestManager() {
        return new EventRequestManager() {
            @Override
            public ClassPrepareRequest createClassPrepareRequest() {
                return null;
            }

            @Override
            public ClassUnloadRequest createClassUnloadRequest() {
                return null;
            }

            @Override
            public ThreadStartRequest createThreadStartRequest() {
                return null;
            }

            @Override
            public ThreadDeathRequest createThreadDeathRequest() {
                return null;
            }

            @Override
            public ExceptionRequest createExceptionRequest(ReferenceType refType, boolean notifyCaught, boolean notifyUncaught) {
                return null;
            }

            @Override
            public MethodEntryRequest createMethodEntryRequest() {
                return null;
            }

            @Override
            public MethodExitRequest createMethodExitRequest() {
                return null;
            }

            @Override
            public MonitorContendedEnterRequest createMonitorContendedEnterRequest() {
                return null;
            }

            @Override
            public MonitorContendedEnteredRequest createMonitorContendedEnteredRequest() {
                return null;
            }

            @Override
            public MonitorWaitRequest createMonitorWaitRequest() {
                return null;
            }

            @Override
            public MonitorWaitedRequest createMonitorWaitedRequest() {
                return null;
            }

            @Override
            public StepRequest createStepRequest(ThreadReference thread, int size, int depth) {
                return null;
            }

            @Override
            public BreakpointRequest createBreakpointRequest(Location location) {
                return null;
            }

            @Override
            public AccessWatchpointRequest createAccessWatchpointRequest(Field field) {
                return null;
            }

            @Override
            public ModificationWatchpointRequest createModificationWatchpointRequest(Field field) {
                return null;
            }

            @Override
            public VMDeathRequest createVMDeathRequest() {
                return null;
            }

            @Override
            public void deleteEventRequest(EventRequest eventRequest) {

            }

            @Override
            public void deleteEventRequests(List<? extends EventRequest> eventRequests) {

            }

            @Override
            public void deleteAllBreakpoints() {

            }

            @Override
            public List<StepRequest> stepRequests() {
                return null;
            }

            @Override
            public List<ClassPrepareRequest> classPrepareRequests() {
                return null;
            }

            @Override
            public List<ClassUnloadRequest> classUnloadRequests() {
                return null;
            }

            @Override
            public List<ThreadStartRequest> threadStartRequests() {
                return null;
            }

            @Override
            public List<ThreadDeathRequest> threadDeathRequests() {
                return null;
            }

            @Override
            public List<ExceptionRequest> exceptionRequests() {
                return null;
            }

            @Override
            public List<BreakpointRequest> breakpointRequests() {
                return null;
            }

            @Override
            public List<AccessWatchpointRequest> accessWatchpointRequests() {
                return null;
            }

            @Override
            public List<ModificationWatchpointRequest> modificationWatchpointRequests() {
                return null;
            }

            @Override
            public List<MethodEntryRequest> methodEntryRequests() {
                return null;
            }

            @Override
            public List<MethodExitRequest> methodExitRequests() {
                return null;
            }

            @Override
            public List<MonitorContendedEnterRequest> monitorContendedEnterRequests() {
                return null;
            }

            @Override
            public List<MonitorContendedEnteredRequest> monitorContendedEnteredRequests() {
                return null;
            }

            @Override
            public List<MonitorWaitRequest> monitorWaitRequests() {
                return null;
            }

            @Override
            public List<MonitorWaitedRequest> monitorWaitedRequests() {
                return null;
            }

            @Override
            public List<VMDeathRequest> vmDeathRequests() {
                return null;
            }

            @Override
            public VirtualMachine virtualMachine() {
                return null;
            }
        };
    }

    @Override
    public BooleanValue mirrorOf(boolean b) {
        return null;
    }

    @Override
    public ByteValue mirrorOf(byte b) {
        return null;
    }

    @Override
    public CharValue mirrorOf(char c) {
        return null;
    }

    @Override
    public ShortValue mirrorOf(short i) {
        return null;
    }

    @Override
    public IntegerValue mirrorOf(int i) {
        return null;
    }

    @Override
    public LongValue mirrorOf(long l) {
        return null;
    }

    @Override
    public FloatValue mirrorOf(float v) {
        return null;
    }

    @Override
    public DoubleValue mirrorOf(double v) {
        return null;
    }

    @Override
    public StringReference mirrorOf(String s) {
        return null;
    }

    @Override
    public VoidValue mirrorOfVoid() {
        return null;
    }

    @Override
    public Process process() {
        return null;
    }

    @Override
    public void dispose() {
        logger.info("videobug dispose called");
    }

    @Override
    public void exit(int i) {
        logger.info("videobug exit called");

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
    public boolean canGetBytecodes() {
        return false;
    }

    @Override
    public boolean canGetSyntheticAttribute() {
        return false;
    }

    @Override
    public boolean canGetOwnedMonitorInfo() {
        return false;
    }

    @Override
    public boolean canGetCurrentContendedMonitor() {
        return false;
    }

    @Override
    public boolean canGetMonitorInfo() {
        return false;
    }

    @Override
    public boolean canUseInstanceFilters() {
        return false;
    }

    @Override
    public boolean canRedefineClasses() {
        return false;
    }

    @Override
    public boolean canAddMethod() {
        return false;
    }

    @Override
    public boolean canUnrestrictedlyRedefineClasses() {
        return false;
    }

    @Override
    public boolean canPopFrames() {
        return false;
    }

    @Override
    public boolean canGetSourceDebugExtension() {
        return false;
    }

    @Override
    public boolean canRequestVMDeathEvent() {
        return false;
    }

    @Override
    public boolean canGetMethodReturnValues() {
        return false;
    }

    @Override
    public boolean canGetInstanceInfo() {
        return false;
    }

    @Override
    public boolean canUseSourceNameFilters() {
        return false;
    }

    @Override
    public boolean canForceEarlyReturn() {
        return false;
    }

    @Override
    public boolean canBeModified() {
        return false;
    }

    @Override
    public boolean canRequestMonitorEvents() {
        return false;
    }

    @Override
    public boolean canGetMonitorFrameInfo() {
        return false;
    }

    @Override
    public boolean canGetClassFileVersion() {
        return false;
    }

    @Override
    public boolean canGetConstantPool() {
        return false;
    }

    @Override
    public String getDefaultStratum() {
        return null;
    }

    @Override
    public void setDefaultStratum(String s) {
        new Exception().printStackTrace();

    }

    @Override
    public long[] instanceCounts(List<? extends ReferenceType> list) {
        return new long[0];
    }

    @Override
    public String description() {
        return "VideoBug Time Travel VM";
    }

    @Override
    public String version() {
        return "1.3.3";
    }

    @Override
    public String name() {
        return "VideoBugVM";
    }

    @Override
    public void setDebugTraceMode(int i) {
        new Exception().printStackTrace();

    }

    @Override
    public VirtualMachine virtualMachine() {
        return this;
    }

    public void setTracePoint(TracePoint tracePoint) throws Exception {
        FilteredDataEventsRequest filterDataEventRequest = tracePoint.toFilterDataEventRequest();
        filterDataEventRequest.setPageSize(1000);

        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Fetching data slice");
        }
        this.replayData = this.project.getService(InsidiousService.class).getClient().fetchDataEvents(filterDataEventRequest);

        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            ProgressIndicatorProvider.getGlobalProgressIndicator().setText2(
                    "Creating frames from: " + this.replayData.getDataEvents().size()
                            + " events across " + this.replayData.getClassInfoMap().size() + " class file, for " +
                            this.replayData.getDataInfoMap().size() + " probes");
        }


        threadReferenceGroup = new InsidiousThreadGroupReference(this, replayData, tracePoint);

        threadReferenceGroup.setThreadReferenceGroup(List.of(new InsidiousThreadReference(threadReferenceGroup, replayData, tracePoint)));

    }

    public void doStep(InsidiousThreadReferenceProxy threadReferenceProxy, int size, int depth, RequestHint requestHint) {
        InsidiousThreadReference threadReference = threadReferenceProxy.getThreadReference();
        threadReference.doStep(size, depth, requestHint);
    }
}
