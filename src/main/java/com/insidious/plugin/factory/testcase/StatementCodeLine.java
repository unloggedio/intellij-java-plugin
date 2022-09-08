package com.insidious.plugin.factory.testcase;

import com.insidious.plugin.factory.CodeLine;

public class StatementCodeLine implements CodeLine {
    private final String statement;

    public StatementCodeLine(String statement) {
        this.statement = statement;
    }

    @Override
    public String getLine() {
        return statement;
    }
}
