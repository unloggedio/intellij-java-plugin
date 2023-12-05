package com.insidious.plugin.pojo.atomic;

public enum TestType {
    UNIT, INTEGRATION, FUZZY, PERFORMANCE, SECURITY;

    @Override
    public String toString() {
        switch (this) {

            case UNIT:
                return "Unit Test";
            case INTEGRATION:
                return "Integration Test";
            case FUZZY:
                return "Fuzz Test";
            case PERFORMANCE:
                return "Performance Test";
            case SECURITY:
                return "Security Test";
        }
        throw new RuntimeException("Undefined test type");
    }
}
