package com.insidious.plugin.assertions;

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
                return "equals(returnValue, expectedValue)";
            case NOT_EQUAL:
                return "notEquals(returnValue, expectedValue)";
            case FALSE:
                return "false(returnValue)";
            case TRUE:
                return "true(returnValue)";
            case LESS_THAN:
                return "lessThan(returnValue, expectedValue)";
            case GREATER_THAN:
                return "greaterThan(returnValue, expectedValue)";
        }
        return "not-defined";
    }
}
