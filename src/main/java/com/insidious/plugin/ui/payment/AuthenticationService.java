package com.insidious.plugin.ui.payment;

import com.insidious.plugin.ui.payment.util.TokenWriterService;

import javax.crypto.Cipher;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

// TODO: handle this gracefully javax.crypto.BadPaddingException: Decryption error

// TODO: handle this gracefully java.lang.IllegalArgumentException: Input byte array has wrong 4-byte ending unit

//TODO: Add a check to ensure one user does not purchase again?


public class AuthenticationService {

    private final String publicKeyPath;
    private final TokenWriterService tokenWriterService;

    public AuthenticationService(String publicKeyPath, TokenWriterService tokenWriterService) {
        this.publicKeyPath = publicKeyPath;
        this.tokenWriterService = tokenWriterService;
    }

    public boolean isTokenValid() {
        try {
            String encryptedToken = tokenWriterService.readToken();
            if (encryptedToken.isEmpty()) {
                return false; // No token stored
            }

            String decryptedToken = decryptToken(encryptedToken, publicKeyPath);
            long expirationTime = Long.parseLong(decryptedToken);
            Date expirationDate = new Date(expirationTime);

            boolean isValid = new Date().before(expirationDate);
            if (!isValid) {
                tokenWriterService.removeToken(); // Remove token if expired
            }

            return isValid;

        } catch (Exception e) {
            e.printStackTrace();
            tokenWriterService.removeToken(); // Remove token if someone reenters the wrong token (ideally wont be able to input wrong token
            return false;
        }
    }

    public boolean validateAndStoreToken(String encryptedToken) {
        try {
            String decryptedToken = decryptToken(encryptedToken, publicKeyPath);
            long expirationTime = Long.parseLong(decryptedToken);
            Date expirationDate = new Date(expirationTime);

            if (new Date().before(expirationDate)) {
                tokenWriterService.writeToken(encryptedToken);
                return true;
            } else {
                tokenWriterService.removeToken(); // Remove token if expired
                return false; // Token expired
            }

        } catch (Exception e) {
            e.printStackTrace();
            tokenWriterService.removeToken(); // Remove token if someone reenters the wrong token (ideally wont be able to input wrong token
            return false;
        }
    }

    private String decryptToken(String encryptedToken, String publicKeyPath) throws Exception {
        // Read the public key file as a string
        String publicKeyPEM = new String(Files.readAllBytes(Paths.get(publicKeyPath)));

        // Remove the first and last lines
        publicKeyPEM = publicKeyPEM
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
