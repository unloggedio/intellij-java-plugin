package com.insidious.plugin.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DiffUtilsTest {

    private static ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testEmptyObject() throws Exception {
        String jsonString = "{}";
        JsonNode input = objectMapper.readTree(jsonString);
        JsonNode expected = objectMapper.createObjectNode();
        assertEquals(expected, DiffUtils.flatten(input));
    }

    @Test
    public void testNestedObject() throws Exception {
        String jsonString = "{\"a\":{\"b\":1,\"c\":{\"d\":3}}}";
        JsonNode input = objectMapper.readTree(jsonString);
        ObjectNode expected = objectMapper.createObjectNode();
        expected.put("/a/b", 1);
        expected.put("/a/c/d", 3);
        assertEquals(expected, DiffUtils.flatten(input));
    }

    @Test
    public void testArray() throws Exception {
        String jsonString = "[1,2,3]";
        JsonNode input = objectMapper.readTree(jsonString);
        ObjectNode expected = objectMapper.createObjectNode();
        expected.put("/0", 1);
        expected.put("/1", 2);
        expected.put("/2", 3);
        assertEquals(expected, DiffUtils.flatten(input));
    }

    @Test
    public void testMixedNestedObjectAndArray() throws Exception {
        String jsonString = "{\"a\":[{\"b\":2},3]}";
        JsonNode input = objectMapper.readTree(jsonString);
        ObjectNode expected = objectMapper.createObjectNode();
        expected.put("/a/0/b", 2);
        expected.put("/a/1", 3);
        assertEquals(expected, DiffUtils.flatten(input));
    }

    @Test
    public void testSimpleKeyValue() throws Exception {
        String jsonString = "{\"a\":1}";
        JsonNode input = objectMapper.readTree(jsonString);
        ObjectNode expected = objectMapper.createObjectNode();
        expected.put("/a", 1);
        assertEquals(expected, DiffUtils.flatten(input));
    }

    @Test
    public void testEmptyArray() throws Exception {
        String jsonString = "[]";
        JsonNode input = objectMapper.readTree(jsonString);
        JsonNode expected = objectMapper.createObjectNode();
        assertEquals(expected, DiffUtils.flatten(input));
    }

    @Test
    public void testComplexNestedStructures() throws Exception {
        String jsonString = "{\"a\":{\"b\":{\"c\":[{\"d\":4},{\"e\":5}]},\"f\":6},\"g\":7}";
        JsonNode input = objectMapper.readTree(jsonString);
        ObjectNode expected = objectMapper.createObjectNode();
        expected.put("/a/b/c/0/d", 4);
        expected.put("/a/b/c/1/e", 5);
        expected.put("/a/f", 6);
        expected.put("/g", 7);
        assertEquals(expected, DiffUtils.flatten(input));
    }

    // Additional tests for edge cases
    @Test
    public void testNestedEmptyObjects() throws Exception {
        String jsonString = "{\"a\":{}}";
        JsonNode input = objectMapper.readTree(jsonString);
        JsonNode expected = objectMapper.createObjectNode();
        assertEquals(expected, DiffUtils.flatten(input));
    }

    @Test
    public void testNestedEmptyArrays() throws Exception {
        String jsonString = "{\"a\":[]}";
        JsonNode input = objectMapper.readTree(jsonString);
        JsonNode expected = objectMapper.createObjectNode();
        assertEquals(expected, DiffUtils.flatten(input));
    }
}