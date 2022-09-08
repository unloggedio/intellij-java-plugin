package com.insidious.plugin.factory.testcase.writer;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;

public class TestScriptWriter {

    private static final Logger logger = LoggerUtil.getInstance(TestScriptWriter.class);

    public static PendingStatement in(ObjectRoutineScript objectRoutine) {
        return new PendingStatement(objectRoutine);
    }

}
