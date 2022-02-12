package extension;

import com.sun.jdi.*;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.request.EventRequestManager;

import java.util.List;
import java.util.Map;

public class InsidiousVirtualMachine implements VirtualMachine {
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
        return null;
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
        return List.of(new InsidiousThreadGroupReference());
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
        return null;
    }

    @Override
    public String version() {
        return "1.6";
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
        return null;
    }
}
