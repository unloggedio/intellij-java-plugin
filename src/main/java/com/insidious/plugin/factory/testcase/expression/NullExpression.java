package com.insidious.plugin.factory.testcase.expression;

import com.insidious.plugin.factory.testcase.TestGenerationState;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScript;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;

public class NullExpression implements Expression {


    NullExpression() {

    }

    @Override
    public String toString() {
        return "NullExpression";
    }

    @Override
    public void writeTo(ObjectRoutineScript objectRoutineScript, TestCaseGenerationConfiguration testConfiguration, TestGenerationState testGenerationState) {
        throw new RuntimeException("null");
    }
}
