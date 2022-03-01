package com.insidious.plugin.extension.model;

public enum DirectionType {
    FORWARDS("insidious_set_reverse_off"),
    BACKWARDS("insidious_set_reverse_on");

    private final String evalStr;

    DirectionType(String evalStr) {
        this.evalStr = evalStr;
    }

    public String getEvaluationExpression() {
        return String.format("\"%s\"", this.evalStr);
    }
}


