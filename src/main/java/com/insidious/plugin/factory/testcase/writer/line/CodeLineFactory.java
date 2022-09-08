package com.insidious.plugin.factory.testcase.writer.line;

public class CodeLineFactory {



    public static CodeLine CommentCodeLine(String statement) {
        return new CommentCodeLine(statement);
    }

    public static CodeLine StatementCodeLine(String statement) {
        return new StatementCodeLine(statement);
    }

}
