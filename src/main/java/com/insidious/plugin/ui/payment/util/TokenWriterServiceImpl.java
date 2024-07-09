//package com.insidious.plugin.ui.payment.util;
//
//import java.io.*;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//
//public class TokenWriterServiceImpl implements TokenWriterService {
//
//    private final String tokenFilePath;
//
//    public TokenWriterServiceImpl(String tokenFilePath) {
//        this.tokenFilePath = tokenFilePath;
//    }
//
//    @Override
//    public String readToken() {
//        try {
//            File tokenFile = new File(tokenFilePath);
//            if (!tokenFile.exists()) {
//                return ""; // No token file exists
//            }
//            return new String(Files.readAllBytes(Paths.get(tokenFilePath))).trim();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//            return "";
//        }
//    }
//
//    @Override
//    public void writeToken(String token) {
//        try (FileWriter writer = new FileWriter(tokenFilePath)) {
//            writer.write(token);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public void removeToken() {
//        File tokenFile = new File(tokenFilePath);
//        if (tokenFile.exists()) {
//            tokenFile.delete();
//        }
//    }
//}
//
