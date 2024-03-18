package com.insidious.plugin.factory;

import com.insidious.plugin.mocking.DeclaredMock;

public class MockManager {
    public MockManager(InsidiousConfigurationState configurationState) {
        this.configurationState = configurationState;
    }

    private final InsidiousConfigurationState configurationState;

    public void disableMock(DeclaredMock declaredMock) {
        configurationState.markMockDisable(declaredMock.getId());
    }

    public void enableMock(DeclaredMock declaredMock) {
        configurationState.markMockActive(declaredMock.getId());
    }

    public boolean isMockActive(DeclaredMock declaredMock) {
        return configurationState.isFieldMockActive(declaredMock.getId());
    }

}
