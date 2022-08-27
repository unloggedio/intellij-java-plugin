package com.insidious.plugin.factory.expression;

public class PlainValueExpression implements Expression {


    private final String value;

    public PlainValueExpression(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
