package com.insidious.plugin.factory.testcase.expression;

import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScript;

public class StringExpression implements Expression {

    private final String value;

    StringExpression(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public void writeTo(ObjectRoutineScript objectRoutineScript) {
        throw new RuntimeException("what");
    }
}
