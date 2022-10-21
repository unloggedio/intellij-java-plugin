package com.insidious.plugin.factory.testcase.writer.line;

public class CodeLineFactory {


    public static CodeLine CommentCodeLine(String statement) {
        if (statement.contains("$")) {
            statement = statement.replace('$', '.');
        }
        return new CommentCodeLine(statement);
    }

    public static CodeLine StatementCodeLine(String statement) {
        return new StatementCodeLine(statement);
    }

}
