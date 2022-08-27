package com.insidious.plugin.factory.expression;

public class StringExpression implements Expression {

    private final String value;

    public StringExpression(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
