package com.insidious.plugin.assertions;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

public enum AssertionType {
    ALLOF,
    ANYOF,
    NOT,
    EQUAL,
    NOT_EQUAL,
    FALSE,
    TRUE,
    LESS_THAN,
    GREATER_THAN;

    @Override
    public String toString() {
        switch (this) {

            case ANYOF:
                return "or";
            case ALLOF:
                return "and";
            case NOT:
                return "NOT";
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
        return "unknown-assertion-type";
    }

    public boolean verify(JsonNode actualValue, JsonNode expectedValue) {
        switch (this) {
            case EQUAL:
                return Objects.equals(actualValue, expectedValue);
            case NOT_EQUAL:
                return !Objects.equals(actualValue, expectedValue);
            case FALSE:
                return Objects.equals(actualValue.asBoolean(), false);
            case TRUE:
                return Objects.equals(actualValue.asBoolean(), true);
            case LESS_THAN:
                return actualValue.asDouble() < expectedValue.asDouble();
            case GREATER_THAN:
                return actualValue.asDouble() > expectedValue.asDouble();
        }
        return false;
    }
}
