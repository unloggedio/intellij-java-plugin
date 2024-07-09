package com.insidious.plugin.ui.payment;

import com.insidious.plugin.factory.InsidiousConfigurationState;

public class StateManager {

    private static final AuthenticationService authenticationService = new AuthenticationService(new InsidiousConfigurationState());

    public static boolean isPremiumUser() {
        return authenticationService.isTokenValid();
    }

    public static  boolean isPremiumUser(String encryptedToken) {
        return authenticationService.validateAndStoreToken(encryptedToken);
    }
}
