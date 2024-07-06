package com.insidious.plugin.ui.payment;

import java.io.*;
import java.util.Properties;

//TODO 1:Add logic for file creation, initial key value creation
//TODO 2: Add the correct business file path
//TODO 3: Instead of true and false, it should store a key and we should validate that key somehow
public class StateManager {
    private static final String PROPERTIES_FILE = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + "premiumFlag.txt";
    private static final String KEY_IS_PREMIUM_USER = "isPremiumUser";

    public static boolean isPremiumUser() {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(PROPERTIES_FILE)) {
            properties.load(input);
            System.out.println("Akshat Jain" + properties.getProperty(KEY_IS_PREMIUM_USER));
            return Boolean.parseBoolean(properties.getProperty(KEY_IS_PREMIUM_USER, "false")); // Default to false
        } catch (IOException ex) {
            ex.printStackTrace();
            return false; // Default to not premium if there's an error
        }
    }

    public static void setPremiumUser(boolean isPremium) {
        Properties properties = new Properties();
        System.out.println("Akshat Jain2" + isPremium);
        System.out.println("Akshat Jain3" + isPremium);
        properties.setProperty(KEY_IS_PREMIUM_USER, Boolean.toString(isPremium));
        System.out.println("Akshat Jain4" + isPremium);
        try (OutputStream output = new FileOutputStream(PROPERTIES_FILE)) {
            properties.store(output, null);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
