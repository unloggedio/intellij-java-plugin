package io.unlogged;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * For GSON
 * Util functions used in test cases for loading JSON files created by Unlogged TestCaseGeneration
 * UnloggedTestUtils.Version: V4
 */
public class UnloggedTestUtils {
    public static final String UNLOGGED_FIXTURES_PATH = "unlogged-fixtures/";
    private final static Gson gson = new GsonBuilder().serializeNulls().create();
    public static String testResourceFilePath = null;
    private static JsonObject sourceObject = null;

    public UnloggedTestUtils(String filePath) throws IOException {
        testResourceFilePath = UNLOGGED_FIXTURES_PATH + filePath + ".json";
        readFileJson();
    }

    public static void LoadResources(Class<?> className, String resourceFileName) throws IOException {
        testResourceFilePath = UNLOGGED_FIXTURES_PATH + className.getSimpleName() + "/" + resourceFileName + ".json";
        readFileJson();
    }

    public static void loadResources(Class<?> className, String resourceFileName) throws IOException {
        testResourceFilePath = UNLOGGED_FIXTURES_PATH + className.getSimpleName() + "/" + resourceFileName + ".json";
        readFileJson();
    }

    public static void readFileJson() throws IOException {
        InputStream inputStream = UnloggedTestUtils.class.getClassLoader().getResourceAsStream(testResourceFilePath);
        assert inputStream != null;
        String stringSource = toString(inputStream, StandardCharsets.UTF_8);
        sourceObject = gson.fromJson(stringSource, JsonObject.class);
    }

    public static String toString(@NotNull InputStream stream) throws IOException {

        char[] buffer = new char[BUFFER_SIZE];
        StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(stream, StandardCharsets.UTF_8);
        for (int numRead; (numRead = in.read(buffer, 0, buffer.length)) > 0; ) {
            out.append(buffer, 0, numRead);
        }
        return out.toString();

    }

    public static <T> T ValueOf(String key, Type type) {
        return valueOf(key, type);
    }

    public static <T> T valueOf(String key, Type type) {
        if (!sourceObject.keySet().contains(key)) {
            return null;
        }
        return gson.fromJson(sourceObject.get(key).toString(), type);
    }

    public static void injectField(Object targetInstance, String name, Object targetObject) throws
            Exception {
        Class<?> aClass;
        if (targetInstance instanceof Class) {
            aClass = (Class) targetInstance;
            while (!aClass.equals(Object.class)) {
                try {
                    Field targetField = aClass.getDeclaredField(name);
                    targetField.setAccessible(true);
                    targetField.set(targetInstance, targetObject);
                } catch (NoSuchFieldException nsfe) {
                    // nothing to set
                }
                aClass = aClass.getSuperclass();
            }

        } else {
            aClass = targetInstance.getClass();

            while (!aClass.equals(Object.class)) {
                try {
                    Field targetField = aClass.getDeclaredField(name);
                    targetField.setAccessible(true);
                    targetField.set(targetInstance, targetObject);
                } catch (NoSuchFieldException nsfe) {
                    // nothing to set
                }
                aClass = aClass.getSuperclass();
            }

        }
    }
}