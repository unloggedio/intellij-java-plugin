package com.insidious.plugin.ui.mocking;

import com.insidious.plugin.mocking.DeclaredMock;

public interface DeclaredMockLifecycleListener {
    void onUpdateRequest(DeclaredMock declaredMock);

    void onDeleteRequest(DeclaredMock declaredMock);

    void onEnable(DeclaredMock declaredMock);

    void onDisable(DeclaredMock declaredMock);
}
