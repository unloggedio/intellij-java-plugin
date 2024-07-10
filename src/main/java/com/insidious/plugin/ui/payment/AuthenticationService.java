package com.insidious.plugin.ui.payment;

import com.insidious.plugin.factory.InsidiousConfigurationState;
import com.insidious.plugin.client.pojo.PremiumTokenData;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import org.json.JSONObject;

public class AuthenticationService {

    private static final String PUBLIC_KEY_PEM =
            "-----BEGIN PUBLIC KEY----- MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyHhfG92s6NpWmuQWhcE16qrVWL+qq4579N7uu7X8Ba+YQJOz1aPf6U5XlgLuNimBmIHR3nixpc/t/aVioI4uKKPpQJqbM/83Zjzakb1mRvZMBTXqt8kfp8G672G9Rkd52VIJbmFhQLg31Q+cCi1psx6N9ApQgXpjJz+W5O/I2kzahRWPz81VEC1xEnKLTxmkrySUIsQSAHAHEHx3gV1sWm2OjJWBooKFpcsO9Y/5B9AJcQHJN6a3yS3BVxMCFDKaDRBknCrWN6mDGS0wOb5iH2CtwogZVx+aPCRaDbC+lnHI7ltxEdUbRd/jIMEYDnU82av1cKA4T1g4O3JR80MIXwIDAQAB -----END PUBLIC KEY-----";
    private final InsidiousConfigurationState configurationState;

    private static final Logger logger = LoggerUtil.getInstance(AuthenticationService.class);

    public AuthenticationService(InsidiousConfigurationState configurationState) {
        this.configurationState = configurationState;
    }

    public boolean isTokenValid() {
        try {
            PremiumTokenData tokenData = configurationState.getPremiumTokenData();
            if (tokenData == null) {
                return false; // No token stored
            }
            String decryptedToken = decryptToken(tokenData.getToken());
            JSONObject tokenJson = new JSONObject(decryptedToken);
            long expirationTime = tokenJson.getLong("expiresAt");
            Date expirationDate = new Date(expirationTime);

            boolean isValid = new Date().before(expirationDate);
            if (!isValid) {
                configurationState.setPremiumTokenData(null); // Remove token if expired
            }
            return isValid;

        } catch (Exception e) {
            logger.info("Token validation failed because of: " + e.getMessage());
            configurationState.setPremiumTokenData(null); // Remove token if decryption fails
            return false;
        }
    }

    public boolean validateAndStoreToken(String encryptedToken) {
        try {
            String decryptedToken = decryptToken(encryptedToken);
            JSONObject tokenJson = new JSONObject(decryptedToken);
            long expirationTime = tokenJson.getLong("expiresAt");
            Date expirationDate = new Date(expirationTime);

            boolean isValid = new Date().before(expirationDate);
            if (isValid) {
                PremiumTokenData tokenData = new PremiumTokenData();
                tokenData.setToken(encryptedToken);
                tokenData.setId(tokenJson.getString("id"));
                tokenData.setCustomerEmail(tokenJson.getString("customerEmail"));
                tokenData.setExpiresAt(expirationTime);
                logger.info("Token validated successfully");

                configurationState.setPremiumTokenData(tokenData);
            } else {
                configurationState.setPremiumTokenData(null); // Remove token if expired
            }
            return isValid;

        } catch (Exception e) {
            logger.info("The token entered is invalid because of: " + e.getMessage());
            return false;
        }
    }

    private String decryptToken(String encryptedToken) throws Exception {
        String publicKeyPEM = PUBLIC_KEY_PEM
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
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
