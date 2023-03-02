package com.insidious.plugin.factory.testcase.writer.line;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.squareup.javapoet.MethodSpec;

public class StatementCodeLine implements CodeLine {
    private static final Logger logger = LoggerUtil.getInstance(StatementCodeLine.class);
    private final String statement;

    StatementCodeLine(String statement) {
        this.statement = statement;
    }

    @Override
    public String getLine() {
        return statement;
    }

    @Override
    public void writeTo(MethodSpec.Builder methodBuilder, Object[] arguments) {
        try {
            methodBuilder.addStatement(statement, arguments);
        } catch (java.lang.IllegalArgumentException iae) {
            logger.error("failed to write statement [" + statement + "] -> [" + arguments + "] ->" + iae.getMessage());
            throw new RuntimeException(iae);
        }
    }

    @Override
    public String toString() {
        return statement + "\n";
    }
}
