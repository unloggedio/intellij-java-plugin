package com.insidious.plugin.assertions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class AssertionEngine {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static AssertionResult executeAssertions(
            AtomicAssertion assertion, JsonNode responseNode
    ) throws JsonProcessingException {
        AssertionResult assertionResult = new AssertionResult();

        AssertionType assertionType = assertion.getAssertionType();

        boolean result;
        if (assertionType == AssertionType.ANYOF) {
            result = false;
            List<AtomicAssertion> subAssertions = assertion.getSubAssertions();
            for (AtomicAssertion subAssertion : subAssertions) {
                AssertionResult subResult = AssertionEngine.executeAssertions(subAssertion, responseNode);
                assertionResult.getResults().putAll(subResult.getResults());
                result = result || subResult.isPassing();
            }

        } else if (assertionType == AssertionType.ALLOF) {

            result = true;
            List<AtomicAssertion> subAssertions = assertion.getSubAssertions();
            for (AtomicAssertion subAssertion : subAssertions) {
                AssertionResult subResult = AssertionEngine.executeAssertions(subAssertion, responseNode);
                assertionResult.getResults().putAll(subResult.getResults());
                result = result && subResult.isPassing();
            }

        } else if (assertionType == AssertionType.NOT) {

            result = true;

            List<AtomicAssertion> subAssertions = assertion.getSubAssertions();
            for (AtomicAssertion subAssertion : subAssertions) {
                AssertionResult subResult = AssertionEngine.executeAssertions(subAssertion, responseNode);
                assertionResult.getResults().putAll(subResult.getResults());
                result = !subResult.isPassing();
            }

        } else {
            JsonNode assertionActualValue = responseNode.at(assertion.getKey());
            Expression expression = assertion.getExpression();
            JsonNode expressedValue = expression.compute(assertionActualValue);
            JsonNode expectedValue = objectMapper.readTree(assertion.getExpectedValue());
            result = assertionType.verify(expressedValue, expectedValue);
        }


        assertionResult.addResult(assertion, result);
        assertionResult.setPassing(result);

        return assertionResult;
    }

}
