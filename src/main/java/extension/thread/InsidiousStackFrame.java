package extension.thread;

import com.sun.jdi.*;
import extension.thread.types.IntegerTypeImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InsidiousStackFrame implements StackFrame {

    private final Location location;
    private final ThreadReference threadReference;
    private final VirtualMachine virtualMachine;
    private final Value value;
    private List<LocalVariable> localVariables;

    public InsidiousStackFrame(Location location,
                               ThreadReference threadReference,
                               List<LocalVariable> localVariables,
                               VirtualMachine virtualMachine) {
        this.location = location;
        this.threadReference = threadReference;
        this.localVariables = localVariables;
        this.virtualMachine = virtualMachine;

        this.localVariables = Arrays.asList(new InsidiousLocalVariable("variable 1", "Integer", "Signature", virtualMachine));
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
