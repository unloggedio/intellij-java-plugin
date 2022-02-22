package extension.thread.types;

import com.sun.jdi.*;
import extension.thread.InsidiousValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InsidiousObjectReference implements ObjectReference {

    private ReferenceType referenceType;
    private final Map<String, InsidiousValue> valueMap;
    private long objectId;
    private final ThreadReference parentThread;

    public InsidiousObjectReference(ThreadReference parentThread) {
        this.valueMap = new HashMap<>();
        this.parentThread = parentThread;
    }

    public void setReferenceType(ReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public void setObjectId(long objectId) {
        this.objectId = objectId;
    }

    @Override
    public ReferenceType referenceType() {
        return referenceType;
    }

    @Override
    public Value getValue(Field field) {
        return valueMap.get(field.name());
    }

    @Override
    public Map<Field, Value> getValues(List<? extends Field> list) {
        Map<Field, Value> values = new HashMap<>();
        for (Field field : list) {
            values.put(field, valueMap.get(field.name()));
        }
        return values;
    }

    public Map<String, InsidiousValue> getValues() {
        return valueMap;
    }

    @Override
    public void setValue(Field field, Value value) throws InvalidTypeException, ClassNotLoadedException {

    }

    @Override
    public Value invokeMethod(ThreadReference threadReference, Method method, List<? extends Value> list, int i) throws InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException, InvocationException {
        return null;
    }

    @Override
    public void disableCollection() {

    }

    @Override
    public void enableCollection() {

    }

    @Override
    public boolean isCollected() {
        return false;
    }

    @Override
    public long uniqueID() {
        return objectId;
    }

    @Override
    public List<ThreadReference> waitingThreads() throws IncompatibleThreadStateException {
        return null;
    }

    @Override
    public ThreadReference owningThread() throws IncompatibleThreadStateException {
        return parentThread;
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
        return referenceType;
    }

    @Override
    public VirtualMachine virtualMachine() {
        return null;
    }
}
