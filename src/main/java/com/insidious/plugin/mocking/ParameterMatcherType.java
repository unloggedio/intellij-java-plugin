package com.insidious.plugin.mocking;

public enum ParameterMatcherType {
    IS, ANY;

    @Override
    public String toString() {
        switch (this) {

            case IS:
                return "IS (exact value match)";
            case ANY:
                return "ANY (type match)";
        }
        return "Invalid parameter matcher";
    }
}
