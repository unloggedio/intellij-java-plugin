package com.insidious.plugin.extension.connector;

import com.insidious.plugin.extension.thread.InsidiousThreadGroupReferenceProxy;
import com.insidious.plugin.extension.thread.InsidiousThreadReference;
import com.insidious.plugin.extension.thread.InsidiousThreadReferenceProxy;
import com.insidious.plugin.extension.thread.InsidiousVirtualMachineProxy;
import com.sun.jdi.ThreadGroupReference;

import java.util.List;
import java.util.stream.Collectors;

public class InsidiousThreadGroupReferenceProxyImpl implements InsidiousThreadGroupReferenceProxy {
    ThreadGroupReference myThreadGroupReference;
    InsidiousVirtualMachineProxy myVmProxy;

    InsidiousThreadGroupReferenceProxyImpl(InsidiousVirtualMachineProxy vmProxy, ThreadGroupReference threadGroupReference) {
        this.myThreadGroupReference = threadGroupReference;
        this.myVmProxy = vmProxy;
    }


    public ThreadGroupReference getThreadGroupReference() {
        return this.myThreadGroupReference;
    }


    public List<InsidiousThreadReferenceProxy> threads() {
        return this.myThreadGroupReference.threads().stream()
                .map(t -> new InsidiousThreadReferenceProxyImpl(this.myVmProxy, (InsidiousThreadReference) t))
                .collect(Collectors.toList());
    }


    public List<InsidiousThreadGroupReferenceProxy> threadGroups() {
        return this.myThreadGroupReference.threadGroups().stream()
                .map(g -> new InsidiousThreadGroupReferenceProxyImpl(this.myVmProxy, g))
                .collect(Collectors.toList());
    }
}


