package com.insidious.plugin.ui.mocking;

public interface UpdateDeleteListener<T> {
    void onUpdateRequest(T object);

    void onDeleteRequest(T object);
}
