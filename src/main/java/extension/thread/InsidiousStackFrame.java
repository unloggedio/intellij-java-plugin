package extension.thread;

import com.sun.jdi.*;
import extension.thread.types.IntegerTypeImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class InsidiousStackFrame implements StackFrame {

    private final ThreadReference threadReference;
    private final VirtualMachine virtualMachine;
    private final Value value;
    private InsidiousLocation location;
    private List<LocalVariable> localVariables;

    public InsidiousStackFrame(InsidiousLocation location,
                               ThreadReference threadReference,
                               VirtualMachine virtualMachine) {
        this.location = location;
        this.threadReference = threadReference;
        this.virtualMachine = virtualMachine;
        this.localVariables = new LinkedList<>();

        value = new InsidiousValue(new IntegerTypeImpl(virtualMachine), virtualMachine);

    }

    @Override
    public Location location() {
        return location;
    }

    @Override
    public ThreadReference thread() {
        return threadReference;
    }

    @Nullable
    @Override
    public ObjectReference thisObject() {
        return null;
    }

    @NotNull
    @Override
    public List<LocalVariable> visibleVariables() throws AbsentInformationException {
        return localVariables;
    }

    public List<LocalVariable> getLocalVariables() {
        return localVariables;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(InsidiousLocation currentLocation) {
        this.location = currentLocation;
    }

    @Nullable
    @Override
    public LocalVariable visibleVariableByName(String s) throws AbsentInformationException {
        return localVariables.get(0);
    }

    @Override
    public Value getValue(LocalVariable localVariable) {
        return value;
    }

    @NotNull
    @Override
    public Map<LocalVariable, Value> getValues(List<? extends LocalVariable> list) {
        return new HashMap<>() {{
            put(localVariables.get(0), value);
        }};
    }

    @Override
    public void setValue(LocalVariable localVariable, Value value) throws InvalidTypeException, ClassNotLoadedException {
        new Exception().printStackTrace();
    }

    @NotNull
    @Override
    public List<Value> getArgumentValues() {
        return Arrays.asList();
    }

    @Override
    public VirtualMachine virtualMachine() {
        return virtualMachine;
    }
}
