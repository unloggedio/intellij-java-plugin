package com.insidious.plugin.assertions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class AssertionEngine {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public AssertionResult executeAssertions(
            List<AtomicAssertion> assertions, JsonNode responseNode
    ) throws JsonProcessingException {
        AssertionResult assertionResult = new AssertionResult();

        boolean assertionPassing = true;
        for (AtomicAssertion assertion : assertions) {
            JsonNode assertionActualValue = responseNode.at(assertion.getKey());
            Expression expression = assertion.getExpression();
            JsonNode expressedValue = expression.compute(assertionActualValue);
            JsonNode expectedValue = objectMapper.readTree(assertion.getExpectedValue());
            boolean result = assertion.getAssertionType().verify(expressedValue, expectedValue);
            assertionPassing = assertionPassing && result;
            assertionResult.addResult(assertion, result);
        }

        assertionResult.setPassing(assertionPassing);

        return assertionResult;
    }

}
