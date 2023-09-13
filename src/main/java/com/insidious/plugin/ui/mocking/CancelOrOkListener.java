package com.insidious.plugin.ui.mocking;

public interface CancelOrOkListener<T> {
    void onCancel();

    void onSave(T object);
}
