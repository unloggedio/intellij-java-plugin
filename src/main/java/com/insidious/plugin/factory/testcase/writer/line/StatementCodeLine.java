package com.insidious.plugin.factory.testcase.writer.line;

import com.squareup.javapoet.MethodSpec;

public class StatementCodeLine implements CodeLine {
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
        methodBuilder.addCode(statement, arguments);
    }
}
