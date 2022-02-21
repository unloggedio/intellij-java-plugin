package extension.thread;

import com.sun.jdi.*;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.request.EventRequestManager;
import extension.connector.RequestHint;
import extension.model.DirectionType;
import extension.model.ReplayData;
import network.Client;
import network.pojo.DataResponse;
import network.pojo.ExecutionSession;
import network.pojo.FilteredDataEventsRequest;
import pojo.TracePoint;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class InsidiousVirtualMachine implements VirtualMachine {

    private final Client client;
    private final ExecutionSession session;
    private ThreadGroupReference threadReferenceGroup;
    private ReplayData replayData;

    public InsidiousVirtualMachine(Client client) {
        this.client = client;
        DataResponse<ExecutionSession> sessions = null;
        try {
            sessions = client.fetchProjectSessions();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.session = sessions.getItems().get(0);
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
        return null;
    }

    @Override
    public EventRequestManager eventRequestManager() {
        return null;
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
        new Exception().printStackTrace();

    }

    @Override
    public void exit(int i) {
        new Exception().printStackTrace();

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
        return "Insidious Time Travel VM";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public String name() {
        return "InsidiousVM";
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
        FilteredDataEventsRequest filterDataEventRequest = FilteredDataEventsRequest.fromTracePoint(tracePoint, DirectionType.BACKWARDS);
        filterDataEventRequest.setPageSize(1000);
        this.replayData = this.client.fetchDataEvents(filterDataEventRequest);
        threadReferenceGroup = new InsidiousThreadGroupReference(this, replayData, tracePoint);
    }

    public void doStep(InsidiousThreadReferenceProxy threadReferenceProxy, int size, int depth, RequestHint requestHint) {
        InsidiousThreadReference threadReference = threadReferenceProxy.getThreadReference();
        threadReference.doStep(size, depth, requestHint);
    }
}
