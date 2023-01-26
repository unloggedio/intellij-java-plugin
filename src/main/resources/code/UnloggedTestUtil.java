package io.unlogged;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

/**
 * For Jackson
 * Util functions used in test cases for loading JSON files created by Unlogged TestCaseGeneration
 * UnloggedTestUtils.Version: V4
 */
public class UnloggedTestUtils {
    public static final String UNLOGGED_FIXTURES_PATH = "unlogged-fixtures/";
    public static final int BUFFER_SIZE = 8192;
    private final static ObjectMapper objectMapper = new ObjectMapper();
    public static String testResourceFilePath = null;
    private static JsonNode sourceObject = null;

    static {
//        register jackson module if they are present
        List<String> jacksonModuleNames = Arrays.asList(
                "com.fasterxml.jackson.datatype.jdk8.Jdk8Module",
                "com.fasterxml.jackson.datatype.joda.JodaModule",
                "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule"
        );

        for (String moduleName : jacksonModuleNames) {
            try {
                //checks for presence of this module class, if not present throws exception
                Class<?> jacksonModuleClass = Class.forName(moduleName);
                objectMapper.registerModule((Module) jacksonModuleClass.getDeclaredConstructor().newInstance());
            } catch (ClassNotFoundException e) {
                // jdk8 module not found
            } catch (InvocationTargetException
                     | InstantiationException
                     | IllegalAccessException
                     | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }


        DateFormat df = new SimpleDateFormat("MMM d, yyyy HH:mm:ss aaa");
        objectMapper.setDateFormat(df);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);
        objectMapper.configure(DeserializationFeature.ACCEPT_FLOAT_AS_INT, true);
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
        String stringSource = toString(inputStream);
        sourceObject = objectMapper.readValue(stringSource, JsonNode.class);
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

    public static <T> T ValueOf(String key, TypeReference typeReference) {
        return valueOf(key, typeReference.getType());
    }

    public static <T> T valueOf(String key, Type type) {
        if (!sourceObject.has(key)) {
            return null;
        }
        try {
            return objectMapper.readValue(sourceObject.get(key).toString(), objectMapper.getTypeFactory().constructType(type));
        } catch (Exception e) {
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
