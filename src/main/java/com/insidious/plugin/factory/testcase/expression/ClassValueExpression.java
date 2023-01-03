package com.insidious.plugin.factory.testcase.expression;

import com.insidious.plugin.factory.testcase.TestGenerationState;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScript;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;

public class ClassValueExpression implements Expression {


    private final String value;

    ClassValueExpression(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public void writeTo(ObjectRoutineScript objectRoutineScript, TestCaseGenerationConfiguration testConfiguration, TestGenerationState testGenerationState) {

    }
}
