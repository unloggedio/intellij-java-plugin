package com.insidious.plugin.ui.mocking;

import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;

public interface OnSaveListener {
    void onSaveDeclaredMock(DeclaredMock declaredMock, MethodUnderTest methodUnderTest);
}
