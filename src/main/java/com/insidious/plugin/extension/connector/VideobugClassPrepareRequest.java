package com.insidious.plugin.extension.connector;

import com.insidious.plugin.extension.thread.InsidiousVirtualMachine;
import com.insidious.plugin.extension.thread.types.InsidiousClassTypeReference;
import com.intellij.debugger.requests.ClassPrepareRequestor;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.ClassPrepareRequest;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class VideobugClassPrepareRequest implements ClassPrepareRequest {
    private List<String> nameSourceFilter = new LinkedList<>();
    private List<String> classExclusionFilter = new LinkedList<>();
    private List<String> classFilter = new LinkedList<>();
    private boolean enable = false;
    private int suspendPolicy = 0;
    private final Map<Object, Object> propertyMap = new HashMap<>();
    private final String classPattern;
    private final ClassPrepareRequestor requestor;
    private final VirtualMachine virtualMachine;

    public VideobugClassPrepareRequest(String classPattern, ClassPrepareRequestor requestor, VirtualMachine virtualMachine) {
        this.classPattern = classPattern;
        this.requestor = requestor;
        this.virtualMachine = virtualMachine;
//        ReferenceType referenceType = virtualMachine.canGetClassFileVersion();
//        this.requestor.processClassPrepare(
//                ((InsidiousVirtualMachine)virtualMachine).process(), refernceType
//        );
    }


    @Override
    public void addClassFilter(ReferenceType refType) {
        classFilter.add(refType.name());

    }

    @Override
    public void addClassFilter(String classPattern) {
        classFilter.add(classPattern);

    }

    @Override
    public void addClassExclusionFilter(String classPattern) {
        classExclusionFilter.add(classPattern);

    }

    @Override
    public void addSourceNameFilter(String sourceNamePattern) {
        nameSourceFilter.add(sourceNamePattern);
    }

    @Override
    public boolean isEnabled() {
        return enable;
    }

    @Override
    public void setEnabled(boolean val) {
        this.enable = val;
    }

    @Override
    public void enable() {
        this.enable = true;
    }

    @Override
    public void disable() {
        this.enable = false;
    }

    @Override
    public void addCountFilter(int count) {
        int countFiler = count;
    }

    @Override
    public void setSuspendPolicy(int policy) {
        suspendPolicy = policy;
    }

    @Override
    public int suspendPolicy() {
        return suspendPolicy;
    }

    @Override
    public void putProperty(Object key, Object value) {
        this.propertyMap.put(key, value);
    }

    @Override
    public Object getProperty(Object key) {
        return this.propertyMap.get(key);
    }

    @Override
    public VirtualMachine virtualMachine() {
        return virtualMachine;
    }

}
