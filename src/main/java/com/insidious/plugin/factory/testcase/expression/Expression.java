package com.insidious.plugin.factory.testcase.expression;

import com.insidious.plugin.factory.testcase.TestGenerationState;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScript;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;

public interface Expression {
    void writeTo(ObjectRoutineScript objectRoutineScript, TestCaseGenerationConfiguration testConfiguration, TestGenerationState testGenerationState);
}
