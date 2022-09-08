package com.insidious.plugin.factory.testcase.writer;

import com.insidious.plugin.callbacks.AgentDownloadUrlCallback;
import com.insidious.plugin.factory.CodeLine;
import com.insidious.plugin.factory.CommentCodeLine;
import com.insidious.plugin.factory.testcase.StatementCodeLine;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.routine.ObjectRoutineContainer;
import com.insidious.plugin.factory.testcase.writer.PendingStatement;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import com.squareup.javapoet.MethodSpec;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * ObjectRoutine is representing a block of code, close to a method, containing all the
 * statements and dependent variables along with their own object routines (the whole hierarchy
 * should be available to recreate this object inside a test case)
 */
public class ObjectRoutineScript {
    private final List<Pair<CodeLine, Object[]>> statements = new LinkedList<>();
    private final String routineName;
    private VariableContainer createdVariables = new VariableContainer();
    private static final Logger logger = LoggerUtil.getInstance(ObjectRoutineScript.class);

    public ObjectRoutineScript() {
        routineName = "<init>";
    }
    public ObjectRoutineScript(String routineName) {
        this.routineName = routineName;
    }


    public void addStatement(String s, Object... args) {
        statements.add(Pair.create(new StatementCodeLine(s), args));
    }

    public void addStatement(String s, List<?> args) {
        Object[] objects = new Object[args.size()];
        for (int i = 0; i < args.size(); i++) {
            Object arg = args.get(i);
            objects[i] = arg;
        }

        statements.add(Pair.create(new StatementCodeLine(s), objects));
    }

    public List<Pair<CodeLine, Object[]>> getStatements() {
        return statements;
    }

    public void addComment(String s, Object... args) {
        statements.add(Pair.create(new CommentCodeLine(s), args));
    }

    public String getRoutineName() {
        return routineName;
    }


    public void addParameterComment(Parameter parameter) {
        addComment("Parameter [" + parameter.getName() + "] => " +
                "Object:" + parameter.getValue() + " of type " + parameter.getType());

    }

    public PendingStatement assignVariable(Parameter testSubject) {
        PendingStatement pendingAssignment = new PendingStatement(this, testSubject);
        return pendingAssignment;
    }

    public VariableContainer getCreatedVariables() {
        return createdVariables;
    }

    public void setCreatedVariables(VariableContainer createdVariables) {
        this.createdVariables = createdVariables;
    }

    public String getName() {
        return routineName;
    }

    public void writeToMethodSpecBuilder(MethodSpec.Builder methodBuilder) {
        for (Pair<CodeLine, Object[]> statement : getStatements()) {
            CodeLine line = statement.getFirst();
            if (line instanceof CommentCodeLine) {
                methodBuilder.addComment(line.getLine(), statement.getSecond());
            } else if (line instanceof StatementCodeLine) {
                methodBuilder.addCode(line.getLine(), statement.getSecond());
            } else {
                logger.error("unknown code line: " + line);
            }
        }
    }
}
