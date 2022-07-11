package com.insidious.plugin.factory;


import com.intellij.openapi.util.Pair;

import java.util.LinkedList;
import java.util.List;

public class StatementContainer {

    private final List<Pair<String, Object[]>> statements = new LinkedList<>();

    public void addStatement(String s,
                             Object... args) {
        statements.add(Pair.create(s, args));
    }

    public List<Pair<String, Object[]>> getStatements() {
        return statements;
    }

    public void addComment(String s) {
        statements.add(Pair.create("// " + s, new Object[]{}));
    }
}
