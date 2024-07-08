package com.insidious.plugin.ui.payment;

import com.insidious.plugin.ui.payment.util.TokenWriterServiceImpl;
import java.io.File;

public class StateManager {

    private static final String PUBLIC_KEY_PATH = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + "public.pem";
    private static final String TOKEN_FILE_PATH = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + "premiumFlag.txt";
    private static final AuthenticationService authenticationService = new AuthenticationService(PUBLIC_KEY_PATH, new TokenWriterServiceImpl(TOKEN_FILE_PATH));

    public static boolean isPremiumUser() {
        return authenticationService.isTokenValid();
    }

    public static  boolean isPremiumUser(String encryptedToken) {
        return authenticationService.validateAndStoreToken(encryptedToken);
    }
}
