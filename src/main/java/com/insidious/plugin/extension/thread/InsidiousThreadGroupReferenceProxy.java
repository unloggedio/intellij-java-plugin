package com.insidious.plugin.extension.thread;

import com.intellij.debugger.engine.jdi.ThreadGroupReferenceProxy;

import java.util.List;

public interface InsidiousThreadGroupReferenceProxy extends ThreadGroupReferenceProxy {
    List<InsidiousThreadReferenceProxy> threads();

    List<InsidiousThreadGroupReferenceProxy> threadGroups();
}
