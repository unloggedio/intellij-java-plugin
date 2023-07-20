package com.insidious.plugin.assertions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class AssertionEngineTest {

    @Test
    void executeAssertions() throws JsonProcessingException {

        AssertionEngine assertionEngine = new AssertionEngine();


        JsonNode actualNode = new ObjectMapper().readTree("""
                {
                    "a":  "1",
                    "b":  "2",
                    "c":  {
                        "a":  "3",
                        "b":  "4"
                    },
                    "nameA": "nameA",
                    "nameB": "nameB",
                    "nameC": "nameC",
                    "nameD": "nameD",
                    "d": "false",
                    "e": "true",
                    "f": [
                        {
                            "a":  "5",
                            "b":  "6",
                            "c":  {
                                "a":  "7",
                                "b":  "8"
                            },
                            "d": "true",
                            "e": "false"
                        }
                    ]
                }
                """);
        List<AtomicAssertion> assertionList = new ArrayList<>();

        AtomicAssertion assertion1 = new AtomicAssertion(AssertionType.EQUAL, "/a", "1");
        AtomicAssertion assertion2 = new AtomicAssertion(AssertionType.EQUAL, "/b", "2");
        AtomicAssertion assertion3 = new AtomicAssertion(AssertionType.LESS_THAN, "/a", "2");
        AtomicAssertion assertion4 = new AtomicAssertion(AssertionType.GREATER_THAN, "/b", "1");

        AtomicAssertion assertion5 = new AtomicAssertion(Expression.SIZE, AssertionType.EQUAL, "/f", "1");
        AtomicAssertion assertion6 = new AtomicAssertion(Expression.SIZE, AssertionType.NOT_EQUAL, "/f", "2");
        AtomicAssertion assertion7 = new AtomicAssertion(Expression.SIZE, AssertionType.GREATER_THAN, "/f", "0");
        AtomicAssertion assertion8 = new AtomicAssertion(Expression.SIZE, AssertionType.LESS_THAN, "/f", "2");
        AtomicAssertion assertion9 = new AtomicAssertion(AssertionType.GREATER_THAN, "/c/b", "1");


        assertionList.add(assertion1);
        assertionList.add(assertion2);
        assertionList.add(assertion3);
        assertionList.add(assertion4);
        assertionList.add(assertion5);
        assertionList.add(assertion6);
        assertionList.add(assertion7);
        assertionList.add(assertion8);
        assertionList.add(assertion9);

        AssertionResult result = assertionEngine.executeAssertions(assertionList, actualNode);

        Assertions.assertTrue(result.getResults().get(assertion1), assertion1.toString());
        Assertions.assertTrue(result.getResults().get(assertion2), assertion2.toString());
        Assertions.assertTrue(result.getResults().get(assertion3), assertion3.toString());
        Assertions.assertTrue(result.getResults().get(assertion4), assertion4.toString());
        Assertions.assertTrue(result.getResults().get(assertion5), assertion5.toString());
        Assertions.assertTrue(result.getResults().get(assertion6), assertion6.toString());
        Assertions.assertTrue(result.getResults().get(assertion7), assertion7.toString());
        Assertions.assertTrue(result.getResults().get(assertion8), assertion8.toString());
        Assertions.assertTrue(result.getResults().get(assertion9), assertion9.toString());
        Assertions.assertTrue(result.isPassing());


    }
}