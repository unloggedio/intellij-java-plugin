package com.insidious.plugin.ui.Components;

public interface OnboardingStateManager {

    public void transistionToState(WaitingStateComponent.WAITING_COMPONENT_STATES state);

    public void checkForSelogs();

    public boolean canGoToDocumentation();
}
