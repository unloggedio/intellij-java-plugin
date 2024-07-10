package com.insidious.plugin.ui.payment;

import com.insidious.plugin.factory.InsidiousConfigurationState;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import org.json.JSONObject;

// TODO: handle this gracefully javax.crypto.BadPaddingException: Decryption error

// TODO: handle this gracefully java.lang.IllegalArgumentException: Input byte array has wrong 4-byte ending unit

public class AuthenticationService {

    private static final String PUBLIC_KEY_PEM =
            "-----BEGIN PUBLIC KEY-----MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvphmzcmTiYx9tKCBZ3rfs7POiS8BFyYPBOPa7R3q2LwZplRxNF7OCBw7ogQlK7608al+JsWQFVBQ6g1eZm+8i2qCKFLYjhItNd33JDonXzNolryx3HJRbcbGxc2mJvu895nXMMW+m0IVLmnflV+7g3ATXjbSe2KTxZu0uzQjD0xlRyqnAgVkr/VGhKemn5e1MXhG6C3f3pQEspsFB5RV7/H6i0WU/4R3t2/NERaj8yjHR/7Bbv88EY0i+7PaNPaC0jf+pqwD6jWP+8X1inw0mSp3pKtpiNp6KLwlGacEar6tyjfU5+HZZSpsR2sNvmgYzeSlhS3ek7VsZRA2pAYo/wIDAQAB-----END PUBLIC KEY-----";
    private final InsidiousConfigurationState configurationState;

    public AuthenticationService(InsidiousConfigurationState configurationState) {
        this.configurationState = configurationState;
    }

    public boolean isTokenValid() {
        try {
            String encryptedToken = configurationState.getPremiumToken();
            System.out.println("Token1 " + encryptedToken);
            if (encryptedToken == null || encryptedToken.isEmpty()) {
                return false; // No token stored
            }

            String decryptedToken = decryptToken(encryptedToken);
            JSONObject tokenJson = new JSONObject(decryptedToken);
            long expirationTime = tokenJson.getLong("expiresAt");
            Date expirationDate = new Date(expirationTime);

            boolean isValid = new Date().before(expirationDate);
            if (!isValid) {
                configurationState.setPremiumToken(null); // Remove token if expired
            }

            return isValid;

        } catch (Exception e) {
            e.printStackTrace();
            configurationState.setPremiumToken(null); // Remove token if decryption fails
            return false;
        }
    }

    public boolean validateAndStoreToken(String encryptedToken) {
        try {
            String decryptedToken = decryptToken(encryptedToken);
            JSONObject tokenJson = new JSONObject(decryptedToken);
            long expirationTime = tokenJson.getLong("expiresAt");
            Date expirationDate = new Date(expirationTime);

            if (new Date().before(expirationDate)) {
                configurationState.setPremiumToken(encryptedToken);
                return true;
            } else {
                configurationState.setPremiumToken(null); // Remove token if expired
                return false; // Token expired
            }

        } catch (Exception e) {
            e.printStackTrace();
            configurationState.setPremiumToken(null); // Remove token if decryption fails
            return false;
        }
    }

    private String decryptToken(String encryptedToken) throws Exception {
        // Remove the first and last lines
        String publicKeyPEM = PUBLIC_KEY_PEM
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", ""); // Remove all whitespace
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyPEM);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(spec);

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedToken));
        return new String(decryptedBytes);
    }
}
