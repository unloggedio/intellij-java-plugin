package com.insidious.plugin.ui.methodscope;

public interface ComponentLifecycleListener<T> {
    void onClose(T component);
}
