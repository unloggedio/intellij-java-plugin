package com.insidious.plugin.assertions;

import java.util.UUID;

public class AtomicAssertion {

    private Expression expression;
    private String expectedValue;
    private String id = UUID.randomUUID().toString();
    private AssertionType assertionType;
    private String key;

    public AtomicAssertion() {
    }

    public AtomicAssertion(Expression expression, AssertionType assertionType, String key, String expectedValue) {
        this.expression = expression;
        this.assertionType = assertionType;
        this.key = key;
        this.expectedValue = expectedValue;
    }

    public AtomicAssertion(AssertionType assertionType, String key, String expectedValue) {
        this.expression = Expression.SELF;
        this.assertionType = assertionType;
        this.key = key;
        this.expectedValue = expectedValue;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public AssertionType getAssertionType() {
        return assertionType;
    }

    public void setAssertionType(AssertionType assertionType) {
        this.assertionType = assertionType;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return "AtomicAssertion{ " +
                expression +
                " (" + key + ")" +
                " " + assertionType +
                " = " + expectedValue +
                " }";
    }
}
