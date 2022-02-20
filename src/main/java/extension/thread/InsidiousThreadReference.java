package extension.thread;

import com.sun.istack.logging.Logger;
import com.sun.jdi.*;
import extension.model.DataInfo;
import extension.model.ReplayData;
import extension.model.StringInfo;
import network.pojo.ClassInfo;
import network.pojo.DataEventWithSessionId;
import pojo.TracePoint;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InsidiousThreadReference implements ThreadReference {


    private static final Logger logger = Logger.getLogger(InsidiousThreadReference.class);
    private final ThreadGroupReference threadGroupReference;
    private final ReplayData replayData;
    private LinkedList<InsidiousStackFrame> stackFrames;

    public InsidiousThreadReference(ThreadGroupReference threadGroupReference,
                                    ReplayData replayData, TracePoint tracePoint) {
        this.threadGroupReference = threadGroupReference;
        this.replayData = replayData;
        calculateFrames();
    }

    private void calculateFrames() {
        LinkedList<InsidiousStackFrame> stackFrames = new LinkedList<>();

        Map<String, DataInfo> dataInfoMap = this.replayData.getDataInfoMap();
        Map<String, ClassInfo> classInfoMap = this.replayData.getClassInfoMap();
        Map<String, StringInfo> stringInfoMap = this.replayData.getStringInfoMap();


        InsidiousStackFrame currentFrame = new InsidiousStackFrame(null, this, this.virtualMachine());

        Map<String, InsidiousLocalVariable> variableMap = new HashMap<>();

        InsidiousLocation currentLocation = null;

        for (DataEventWithSessionId dataEvent : this.replayData.getDataEvents()) {
            DataInfo probeInfo = dataInfoMap.get(String.valueOf(dataEvent.getDataId()));
            logger.info("Build frame from event type [" + probeInfo.getEventType() + "]");

            switch (probeInfo.getEventType()) {
                case LINE_NUMBER:
                    ClassInfo classInfo = classInfoMap.get(String.valueOf(probeInfo.getClassId()));
                    currentLocation = new InsidiousLocation(classInfo.getFilename(), probeInfo.getLine());
                    if (currentFrame.location() == null) {
                        currentFrame.setLocation(currentLocation);
                    }
                    currentLocation = null;
                    break;

                case METHOD_ENTRY:
                    stackFrames.add(currentFrame);
                    currentFrame = new InsidiousStackFrame(currentLocation, this, this.virtualMachine());
                    break;
                case LOCAL_STORE:
                    String variableName = probeInfo.getAttribute("Name", null);
                    String variableType = probeInfo.getAttribute("Type", null);

                    if (variableMap.containsKey(variableName)) {
                        continue;
                    }

                    Object value = dataEvent.getValue();

                    if (variableType.contains("java/lang/String")) {
                        StringInfo stringInfo = stringInfoMap.get(String.valueOf(value));
                        if (stringInfo != null) {
                            value = stringInfo.getContent();
                        }
                    }

                    InsidiousLocalVariable newVariable = new InsidiousLocalVariable(
                            variableName,
                            variableType,
                            variableType,
                            value,
                            this.virtualMachine());

                    currentFrame.getLocalVariables().add(newVariable);
                    variableMap.put(variableName, newVariable);
                    break;


            }

        }
        this.stackFrames = stackFrames;

    }

    @Override
    public String name() {
        return "Insidious thread reference";
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
    public int suspendCount() {
        return 0;
    }

    @Override
    public void stop(ObjectReference objectReference) throws InvalidTypeException {
        new Exception().printStackTrace();

    }

    @Override
    public void interrupt() {
        new Exception().printStackTrace();

    }

    @Override
    public int status() {
        return ThreadReference.THREAD_STATUS_MONITOR;
    }

    @Override
    public boolean isSuspended() {
        return true;
    }

    @Override
    public boolean isAtBreakpoint() {
        return true;
    }

    @Override
    public ThreadGroupReference threadGroup() {
        return this.threadGroupReference;
    }

    @Override
    public int frameCount() throws IncompatibleThreadStateException {
        return stackFrames.size();
    }

    @Override
    public List<StackFrame> frames() throws IncompatibleThreadStateException {
        return stackFrames.stream().collect(Collectors.toList());
    }

    @Override
    public StackFrame frame(int i) throws IncompatibleThreadStateException {
        return stackFrames.get(i);
    }

    @Override
    public List<StackFrame> frames(int i, int i1) throws IncompatibleThreadStateException {
        return stackFrames.subList(i, i1).stream().collect(Collectors.toList());
    }

    @Override
    public List<ObjectReference> ownedMonitors() throws IncompatibleThreadStateException {
        return null;
    }

    @Override
    public List<MonitorInfo> ownedMonitorsAndFrames() throws IncompatibleThreadStateException {
        return null;
    }

    @Override
    public ObjectReference currentContendedMonitor() throws IncompatibleThreadStateException {
        return null;
    }

    @Override
    public void popFrames(StackFrame stackFrame) throws IncompatibleThreadStateException {
        new Exception().printStackTrace();

    }

    @Override
    public void forceEarlyReturn(Value value) throws InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException {
        new Exception().printStackTrace();

    }

    @Override
    public ReferenceType referenceType() {
        return null;
    }

    @Override
    public Value getValue(Field field) {
        return null;
    }

    @Override
    public Map<Field, Value> getValues(List<? extends Field> list) {
        return null;
    }

    @Override
    public void setValue(Field field, Value value) throws InvalidTypeException, ClassNotLoadedException {
        new Exception().printStackTrace();

    }

    @Override
    public Value invokeMethod(ThreadReference threadReference, Method method, List<? extends Value> list, int i) throws InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException, InvocationException {
        return null;
    }

    @Override
    public void disableCollection() {
        new Exception().printStackTrace();

    }

    @Override
    public void enableCollection() {
        new Exception().printStackTrace();

    }

    @Override
    public boolean isCollected() {
        return false;
    }

    @Override
    public long uniqueID() {
        return 0;
    }

    @Override
    public List<ThreadReference> waitingThreads() throws IncompatibleThreadStateException {
        return null;
    }

    @Override
    public ThreadReference owningThread() throws IncompatibleThreadStateException {
        return null;
    }

    @Override
    public int entryCount() throws IncompatibleThreadStateException {
        return 0;
    }

    @Override
    public List<ObjectReference> referringObjects(long l) {
        return null;
    }

    @Override
    public Type type() {
        return null;
    }

    @Override
    public VirtualMachine virtualMachine() {
        return threadGroupReference.virtualMachine();
    }
}
