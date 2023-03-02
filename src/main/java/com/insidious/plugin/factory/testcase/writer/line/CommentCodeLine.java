package com.insidious.plugin.factory.testcase.writer.line;

import com.squareup.javapoet.MethodSpec;

public class CommentCodeLine implements CodeLine {
    CommentCodeLine(String statement) {
        this.statement = statement;
    }

    private final String statement;

    @Override
    public String getLine() {
        return statement;
    }

    @Override
    public void writeTo(MethodSpec.Builder methodBuilder, Object[] arguments) {
        methodBuilder.addComment(statement, arguments);
    }

    @Override
    public String toString() {
        return "//" + statement + "\n";
    }
}
