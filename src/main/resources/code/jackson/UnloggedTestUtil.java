package io.unlogged;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import org.nustaq.serialization.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;

/**
 * Util functions used in test cases for loading JSON files created by Unlogged TestCaseGeneration
 * UnloggedTestUtils.Version: V2
 */
public class UnloggedTestUtils {
    public static final String UNLOGGED_FIXTURES_PATH = "unlogged-fixtures/";
    private final static ObjectMapper objectMapper = new ObjectMapper();
    public static String testResourceFilePath = null;
    private static JsonNode sourceObject = null;

    static {
        DateFormat df = new SimpleDateFormat("MMM d, yyyy HH:mm:ss aaa");
        objectMapper.setDateFormat(df);
    }

    public UnloggedTestUtils(String filePath) throws IOException {
        testResourceFilePath = UNLOGGED_FIXTURES_PATH + filePath + ".json";
        readFileJson();
    }

    public static void LoadResources(Class<?> className, String patientLeadService) throws IOException {
        testResourceFilePath = UNLOGGED_FIXTURES_PATH + className.getSimpleName() + "/" + patientLeadService + ".json";
        readFileJson();
    }

    public static void loadResources(Class<?> className, String patientLeadService) throws IOException {
        testResourceFilePath = UNLOGGED_FIXTURES_PATH + className.getSimpleName() + "/" + patientLeadService + ".json";
        readFileJson();
    }

    public static void readFileJson() throws IOException {
        InputStream inputStream = UnloggedTestUtils.class.getClassLoader().getResourceAsStream(testResourceFilePath);
        assert inputStream != null;
        String stringSource = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        sourceObject = objectMapper.readValue(stringSource, JsonNode.class);
    }

    public static <T> T ValueOf(String key, Type type) {
        return valueOf(key, type);
    }

    public static <T> T valueOf(String key, Type type) {
        if (!sourceObject.has(key)) {
            return null;
        }
        try {
            return objectMapper.readValue(sourceObject.get(key).toString(), objectMapper.getTypeFactory().constructType(type));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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