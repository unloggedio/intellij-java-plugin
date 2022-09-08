package com.insidious.plugin.factory.testcase.expression;

public class StringExpression implements Expression {

    private final String value;

    StringExpression(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
