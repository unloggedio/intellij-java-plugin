package com.insidious.plugin.assertions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AssertionTypeTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testArrayContainsStringField() throws JsonProcessingException {
        JsonNode actualNode = objectMapper.readTree("[\"a1\", \"a2\", \"a3\"]");
        JsonNode expectedNode = objectMapper.readTree("\"a2\"");
        boolean result = AssertionType.CONTAINS_ITEM.verify(actualNode, expectedNode);
        Assertions.assertTrue(result);
    }

    @Test
    public void testArrayContainsObjectField() throws JsonProcessingException {
        JsonNode actualNode = objectMapper.readTree(
                """
                        [
                          "a1",
                          "a2",
                          "a3",
                          {
                            "b1": "c1",
                            "d3": "d2"
                          },
                          {
                            "b2": "c2",
                            "d5": "d6"
                          }
                        ]
                                                """
        );
        JsonNode expectedNode = objectMapper.readTree("""
                {
                  "b1": "c1",
                  "d3": "d2"
                }
                """);
        boolean result = AssertionType.CONTAINS_ITEM.verify(actualNode, expectedNode);
        Assertions.assertTrue(result);
    }

    @Test
    public void testArrayNotContainsObjectField() throws JsonProcessingException {
        JsonNode actualNode = objectMapper.readTree(
                """
                        [
                          "a1",
                          "a2",
                          "a3",
                          {
                            "b1": "c1",
                            "d3": "d2"
                          },
                          {
                            "b2": "c2",
                            "d5": "d6"
                          }
                        ]
                                                """
        );
        JsonNode expectedNode = objectMapper.readTree("""
                {
                  "b1": "c6",
                  "d3": "d2"
                }
                """);
        boolean result = AssertionType.NOT_CONTAINS_ITEM.verify(actualNode, expectedNode);
        Assertions.assertTrue(result);
    }

    @Test
    public void testObjectContainsKeyField() throws JsonProcessingException {
        JsonNode actualNode = objectMapper.readTree(
                """
                        {
                          "b1": "c1",
                          "d3": "d2",
                          "d5": "d2",
                          "d6": "d2",
                          "d7": "d2"
                        }
                        """
        );
        JsonNode expectedNode = objectMapper.readTree("""
                "d6"
                """);
        boolean result = AssertionType.CONTAINS_KEY.verify(actualNode, expectedNode);
        Assertions.assertTrue(result);
    }

    @Test
    public void testObjectNotContainsKeyField() throws JsonProcessingException {
        JsonNode actualNode = objectMapper.readTree(
                """
                        {
                          "b1": "c1",
                          "d3": "d2",
                          "d5": "d2",
                          "d6": "d2",
                          "d7": "d2"
                        }
                        """
        );
        JsonNode expectedNode = objectMapper.readTree("""
                "d8"
                """);
        boolean result = AssertionType.NOT_CONTAINS_KEY.verify(actualNode, expectedNode);
        Assertions.assertTrue(result);
    }

    @Test
    public void testStringContainsStringField() throws JsonProcessingException {
        JsonNode actualNode = objectMapper.readTree(
                """
                        "asdfiajofajsdofjsofajsfioajspf"
                        """
        );
        JsonNode expectedNode = objectMapper.readTree("""
                "fajsdof"
                """);
        boolean result = AssertionType.CONTAINS_STRING.verify(actualNode, expectedNode);
        Assertions.assertTrue(result);
    }

    @Test
    public void testStringNotContainsStringField() throws JsonProcessingException {
        JsonNode actualNode = objectMapper.readTree(
                """
                        "asdfiajofajsdofjsofajsfioajspf"
                        """
        );
        JsonNode expectedNode = objectMapper.readTree("""
                "fa1jsdof"
                """);
        boolean result = AssertionType.CONTAINS_STRING.verify(actualNode, expectedNode);
        Assertions.assertFalse(result);
    }

    @Test
    public void testStringNotNotContainsStringField() throws JsonProcessingException {
        JsonNode actualNode = objectMapper.readTree(
                """
                        "asdfiajofajsdofjsofajsfioajspf"
                        """
        );
        JsonNode expectedNode = objectMapper.readTree("""
                "fajsd1of"
                """);
        boolean result = AssertionType.NOT_CONTAINS_STRING.verify(actualNode, expectedNode);
        Assertions.assertTrue(result);
    }

    @Test
    public void testStringMatchesRegex1() throws JsonProcessingException {
        JsonNode expectedNode = objectMapper.readTree("\"\\\\d{5}(-\\\\d{4})?\"");
        JsonNode actualNode = objectMapper.readTree("""
                "30002"
                """);
        boolean result = AssertionType.MATCHES_REGEX.verify(actualNode, expectedNode);
        Assertions.assertTrue(result);
    }

    @Test
    public void testStringMatchesRegex2() throws JsonProcessingException {
        JsonNode expectedNode = objectMapper.readTree("\"\\\\d{5}(-\\\\d{4})?\"");
        JsonNode actualNode = objectMapper.readTree("""
                "30002-7368"
                """);
        boolean result = AssertionType.MATCHES_REGEX.verify(actualNode, expectedNode);
        Assertions.assertTrue(result);
    }

    @Test
    public void testStringMatchesRegex4() throws JsonProcessingException {
        JsonNode expectedNode = objectMapper.readTree("123");
        JsonNode actualNode = objectMapper.readTree("123");
        boolean result = AssertionType.MATCHES_REGEX.verify(actualNode, expectedNode);
        Assertions.assertTrue(result);
    }

    @Test
    public void testStringMatchesRegex5() throws JsonProcessingException {
        JsonNode expectedNode = objectMapper.readTree("\"123\"");
        JsonNode actualNode = objectMapper.readTree("\"123\"");
        boolean result = AssertionType.MATCHES_REGEX.verify(actualNode, expectedNode);
        Assertions.assertTrue(result);
    }

    @Test
    public void testStringNotMatchesRegex2() throws JsonProcessingException {
        JsonNode expectedNode = objectMapper.readTree("\"\\\\d{5}(-\\\\d{4})?\"");
        JsonNode actualNode = objectMapper.readTree("""
                "30002-73681"
                """);
        boolean result = AssertionType.MATCHES_REGEX.verify(actualNode, expectedNode);
        Assertions.assertFalse(result);
    }

    @Test
    public void testStringNotNotMatchesRegex3() throws JsonProcessingException {
        JsonNode expectedNode = objectMapper.readTree("\"\\\\d{5}(-\\\\d{4})?\"");
        JsonNode actualNode = objectMapper.readTree("""
                "30002-73681"
                """);
        boolean result = AssertionType.NOT_MATCHES_REGEX.verify(actualNode, expectedNode);
        Assertions.assertTrue(result);
    }

}