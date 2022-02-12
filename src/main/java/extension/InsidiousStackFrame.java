package extension;

import com.sun.jdi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class InsidiousStackFrame implements StackFrame {
    @Override
    public Location location() {
        return null;
    }

    @Override
    public ThreadReference thread() {
        return null;
    }

    @Nullable
    @Override
    public ObjectReference thisObject() {
        return null;
    }

    @NotNull
    @Override
    public List<LocalVariable> visibleVariables() throws AbsentInformationException {
        return null;
    }

    @Nullable
    @Override
    public LocalVariable visibleVariableByName(String s) throws AbsentInformationException {
        return null;
    }

    @Override
    public Value getValue(LocalVariable localVariable) {
        return null;
    }

    @NotNull
    @Override
    public Map<LocalVariable, Value> getValues(List<? extends LocalVariable> list) {
        return null;
    }

    @Override
    public void setValue(LocalVariable localVariable, Value value) throws InvalidTypeException, ClassNotLoadedException {
        new Exception().printStackTrace();
    }

    @NotNull
    @Override
    public List<Value> getArgumentValues() {
        return null;
    }

    @Override
    public VirtualMachine virtualMachine() {
        return null;
    }
}
