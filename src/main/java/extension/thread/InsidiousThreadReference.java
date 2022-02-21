package extension.thread;

import com.intellij.openapi.diagnostic.Logger;
import com.sun.jdi.*;
import extension.connector.RequestHint;
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


    private static final Logger logger = Logger.getInstance(InsidiousThreadReference.class);
    private final ThreadGroupReference threadGroupReference;
    private final ReplayData replayData;
    private Integer position;
    private LinkedList<InsidiousStackFrame> stackFrames;

    public InsidiousThreadReference(ThreadGroupReference threadGroupReference,
                                    ReplayData replayData, TracePoint tracePoint) {
        this.threadGroupReference = threadGroupReference;
        this.replayData = replayData;
        position = 0;
        calculateFrames();
    }

    private void calculateFrames() {
        LinkedList<InsidiousStackFrame> stackFrames = new LinkedList<>();

        Map<String, DataInfo> dataInfoMap = this.replayData.getDataInfoMap();
        Map<String, ClassInfo> classInfoMap = this.replayData.getClassInfoMap();
        Map<String, StringInfo> stringInfoMap = this.replayData.getStringInfoMap();


        InsidiousStackFrame currentFrame = new InsidiousStackFrame(null, this, this.virtualMachine());

        Map<String, InsidiousLocalVariable> variableMap = new HashMap<>();

//        InsidiousLocation currentLocation = null;

        int methodsToSkip = 0;
        List<DataEventWithSessionId> dataEventsList = this.replayData.getDataEvents();
        List<DataEventWithSessionId> subList = dataEventsList.subList(position, dataEventsList.size());
        int currentClassId = -1; // dataInfoMap.get(String.valueOf(subList.get(0).getDataId())).getClassId();

        for (int index = 0; index < subList.size(); index++) {
            DataEventWithSessionId dataEvent = subList.get(index);
            DataInfo probeInfo = dataInfoMap.get(String.valueOf(dataEvent.getDataId()));
            int classId = probeInfo.getClassId();
            ClassInfo classInfo = classInfoMap.get(String.valueOf(classId));
            if (currentClassId != -1 && currentClassId != classId) {
                continue;
            }


            System.out.println("[" + (index + position) + "] Build frame from event type [" + dataEvent.getNanoTime() + "] line [" + probeInfo.getLine() + "][" + probeInfo.getEventType() + "]  of class [" + classInfo.getFilename() + "]");

            switch (probeInfo.getEventType()) {
                case LINE_NUMBER:

                    if (methodsToSkip == 0) {
                        currentClassId = classId;
                        if (currentFrame.location() == null) {
                            InsidiousLocation currentLocation = new InsidiousLocation(classInfo.getFilename(), probeInfo.getLine() - 1);
                            currentFrame.setLocation(currentLocation);
                        }
//                        currentLocation = null;
                    }
                    break;

                case METHOD_NORMAL_EXIT:
                    methodsToSkip++;
                    break;
//                    currentFrame = stackFrames.pop();

                case METHOD_ENTRY:
                    if (methodsToSkip == 0) {
                        stackFrames.add(currentFrame);
                        currentFrame = new InsidiousStackFrame(null, this, this.virtualMachine());
                    } else {
                        methodsToSkip--;
                    }
                    break;
                case LOCAL_STORE:
                case LOCAL_LOAD:


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

        if (currentFrame.location() != null) {
            stackFrames.add(currentFrame);
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

    public void doStep(int size, int depth, RequestHint requestHint) {

        List<DataEventWithSessionId> dataEvents = replayData.getDataEvents();
        int currentLineNumber = replayData.getDataInfoMap().get(String.valueOf(dataEvents.get(position).getDataId())).getLine();
        List<DataEventWithSessionId> subList = dataEvents.subList(position, dataEvents.size());
        if (size < 0) {
            for (int i = position; i < dataEvents.size(); i++) {
                DataEventWithSessionId dataEventWithSessionId = dataEvents.get(i);
                DataInfo dataInfo = replayData.getDataInfoMap().get(String.valueOf(dataEventWithSessionId.getDataId()));
                if (dataInfo.getLine() != currentLineNumber) {
                    position = i;
                    break;
                }
            }
        } else {
            for (int i = position; i > 0; i--) {
                DataEventWithSessionId dataEventWithSessionId = dataEvents.get(i);
                DataInfo dataInfo = replayData.getDataInfoMap().get(String.valueOf(dataEventWithSessionId.getDataId()));
                if (dataInfo.getLine() != currentLineNumber) {
                    position = i;
                    break;
                }
            }
        }


        calculateFrames();
    }
}
