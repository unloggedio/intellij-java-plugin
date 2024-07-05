package com.insidious.plugin.util;


import java.io.*;
import java.nio.charset.StandardCharsets;

public class StreamUtil {
    public static final int BUFFER_SIZE = 8192;

    public static int copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        int total = 0;
        while ((read = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, read);
            total += read;
        }
        return total;
    }

    public static String streamToString(InputStream stream) throws IOException {
        char[] buffer = new char[BUFFER_SIZE];
        StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(stream, StandardCharsets.UTF_8);
        for (int numRead; (numRead = in.read(buffer, 0, buffer.length)) > 0; ) {
            out.append(buffer, 0, numRead);
        }
        return out.toString();
    }


    public static byte[] streamToBytes(InputStream stream) throws IOException {
        char[] buffer = new char[BUFFER_SIZE];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Reader in = new InputStreamReader(stream, StandardCharsets.UTF_8);
        for (int numRead; (numRead = in.read(buffer, 0, buffer.length)) > 0; ) {
            for (int i = 0; i < numRead; i++) {
                char c = buffer[i];
                out.write(c);
            }
        }
        return out.toByteArray();
    }


}
