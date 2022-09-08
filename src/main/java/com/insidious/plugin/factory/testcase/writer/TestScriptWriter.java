package com.insidious.plugin.factory.testcase.writer;

import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.testcase.routine.ObjectRoutine;
import com.insidious.plugin.factory.candidate.TestCandidateMetadata;
import com.insidious.plugin.factory.testcase.parameter.ParameterFactory;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.expression.Expression;
import com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory;
import com.insidious.plugin.factory.testcase.expression.StringExpression;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;

import java.util.HashMap;
import java.util.Map;

public class TestScriptWriter {

    private static final Logger logger = LoggerUtil.getInstance(TestScriptWriter.class);

    public static PendingStatement in(ObjectRoutineScript objectRoutine) {
        return new PendingStatement(objectRoutine);
    }

}
