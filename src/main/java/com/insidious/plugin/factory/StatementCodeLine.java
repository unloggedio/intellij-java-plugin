package com.insidious.plugin.factory;

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
