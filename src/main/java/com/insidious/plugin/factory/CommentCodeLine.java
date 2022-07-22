package com.insidious.plugin.factory;

public class CommentCodeLine implements CodeLine {
    public CommentCodeLine(String statement) {
        this.statement = statement;
    }

    private final String statement;

    @Override
    public String getLine() {
        return statement;
    }
}
