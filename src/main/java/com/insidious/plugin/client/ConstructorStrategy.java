package com.insidious.plugin.client;

public enum ConstructorStrategy {
    CONSTRUCTOR,
    JSON_DESERIALIZE,
    MANUAL_CODE;

    @Override
    public String toString() {
        switch (this) {
            case CONSTRUCTOR:
                return "Constructor";
            case JSON_DESERIALIZE:
                return "JSON value deserialization";
            case MANUAL_CODE:
                return "Create manually";
        }
        return "Unknown";
    }
}
