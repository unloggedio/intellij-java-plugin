package io.unlogged;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Util functions used in test cases for loading JSON files created by Unlogged TestCaseGeneration
 * UnloggedTestUtils.Version: V3
 */
public class UnloggedTestUtils {
    public static final String UNLOGGED_FIXTURES_PATH = "unlogged-fixtures/";
    private final static Gson gson = new GsonBuilder().serializeNulls().create();
    public static String testResourceFilePath = null;
    private static JsonObject sourceObject = null;

    static {
        DateFormat df = new SimpleDateFormat("MMM d, yyyy HH:mm:ss aaa");
        objectMapper.setDateFormat(df);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector() {
            @Override
            public boolean hasIgnoreMarker(AnnotatedMember m) {
                return false;
            }
        });

    }

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
        String stringSource = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        sourceObject = gson.fromJson(stringSource, JsonObject.class);
    }

    public static <T> T ValueOf(String key, Type type) {
        if (!sourceObject.keySet().contains(key)) {
            return null;
        }
        return gson.fromJson(sourceObject.get(key).toString(), type);
    }
    public static <T> T ValueOf(String key, TypeReference typeReference) {
        return valueOf(key, typeReference.getType());
    }

    public static <T> T ValueOf(String key, TypeReference<T> typeRef) throws JsonProcessingException {
        if (!sourceObject.keySet().contains(key)) {
            return null;
        }

        Jdk8Module jdk8Module = new Jdk8Module();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(jdk8Module);
        return mapper.readValue(sourceObject.get(key).toString(), typeRef);
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