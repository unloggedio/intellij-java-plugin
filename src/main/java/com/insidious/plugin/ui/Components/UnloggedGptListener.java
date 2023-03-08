package com.insidious.plugin.ui.Components;

public interface UnloggedGptListener {

    void triggerCallOfType(String type);

    void refreshPage();

    void goBack();

    void testApiCall();

    String makeApiCallForPrompt(String currentPrompt);

    void triggerCallTypeForCurrentMethod(String type);
}
