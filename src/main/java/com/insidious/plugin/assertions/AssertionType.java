package com.insidious.plugin.assertions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Objects;

public enum AssertionType {
    EQUAL,
    NOT_EQUAL,
    FALSE,
    TRUE,
    LESS_THAN,
    GREATER_THAN;

    @Override
    public String toString() {
        switch (this) {

            case EQUAL:
                return "equals";
            case NOT_EQUAL:
                return "notEquals";
            case FALSE:
                return "false";
            case TRUE:
                return "true";
            case LESS_THAN:
                return "lessThan";
            case GREATER_THAN:
                return "greaterThan";
        }
        return "not-defined";
    }

    public boolean verify(JsonNode actualValue, String expectedValue) {
        switch (this) {
            case EQUAL:
                return Objects.equals(actualValue.asText(), expectedValue);
            case NOT_EQUAL:
                return !Objects.equals(actualValue.asText(), expectedValue);
            case FALSE:
                return Objects.equals(actualValue.asBoolean(), false);
            case TRUE:
                return Objects.equals(actualValue.asBoolean(), true);
            case LESS_THAN:
                return actualValue.asDouble() < Double.parseDouble(expectedValue);
            case GREATER_THAN:
                return actualValue.asDouble() > Double.parseDouble(expectedValue);
        }
        return false;
    }
}
