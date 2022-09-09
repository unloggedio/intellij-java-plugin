package com.insidious.plugin.factory.testcase.writer;

import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.writer.line.CodeLine;
import com.insidious.plugin.factory.testcase.writer.line.CodeLineFactory;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Modifier;
import java.lang.reflect.Type;
import java.util.*;

/**
 * ObjectRoutine is representing a block of code, close to a method, containing all the
 * statements and dependent variables along with their own object routines (the whole hierarchy
 * should be available to recreate this object inside a test case)
 */
public class ObjectRoutineScript {
    private static final Logger logger = LoggerUtil.getInstance(ObjectRoutineScript.class);
    private final List<Pair<CodeLine, Object[]>> statements = new LinkedList<>();
    private final Set<TypeName> exceptions = new HashSet<>();
    private final Set<AnnotationSpec> annotations = new HashSet<>();
    private final Set<Modifier> modifiers = new HashSet<>();
    private String routineName;
    private VariableContainer createdVariables = new VariableContainer();

    public ObjectRoutineScript() {
        routineName = "<init>";
    }

    public ObjectRoutineScript(String routineName) {
        this.routineName = routineName;
    }

    public ObjectRoutineScript(VariableContainer createdVariables) {
        this.createdVariables = createdVariables;
    }

    public void addStatement(String s, Object... args) {
        statements.add(Pair.create(CodeLineFactory.StatementCodeLine(s), args));
    }

    public void addStatement(String s, List<?> args) {
        Object[] objects = new Object[args.size()];
        for (int i = 0; i < args.size(); i++) {
            Object arg = args.get(i);
            objects[i] = arg;
        }

        statements.add(Pair.create(CodeLineFactory.StatementCodeLine(s), objects));
    }

    public List<Pair<CodeLine, Object[]>> getStatements() {
        return statements;
    }

    public void addComment(String s, Object... args) {
        statements.add(Pair.create(CodeLineFactory.CommentCodeLine(s), args));
    }

    public String getRoutineName() {
        return routineName;
    }

    public void setRoutineName(String routineName) {
        this.routineName = routineName;
    }

    public void addParameterComment(Parameter parameter) {
        addComment("Parameter [" + parameter.getName() + "] => " +
                "Object:" + parameter.getValue() + " of type " + parameter.getType());

    }

    public PendingStatement assignVariable(Parameter testSubject) {
        PendingStatement pendingAssignment = new PendingStatement(this, testSubject);
        return pendingAssignment;
    }

    public String getName() {
        return routineName;
    }

    public void writeToMethodSpecBuilder(MethodSpec.Builder methodBuilder) {
        for (Pair<CodeLine, Object[]> statement : getStatements()) {
            statement.getFirst().writeTo(methodBuilder, statement.getSecond());
        }
    }

    public MethodSpec.Builder toMethodSpec() {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(this.routineName);

        methodBuilder.addExceptions(this.exceptions);
        methodBuilder.addAnnotations(this.annotations);
        methodBuilder.addModifiers(this.modifiers);

        for (Pair<CodeLine, Object[]> statement : getStatements()) {
            statement.getFirst().writeTo(methodBuilder, statement.getSecond());
        }
        return methodBuilder;
    }

    public VariableContainer getCreatedVariables() {
        return createdVariables;
    }

    public void setCreatedVariables(VariableContainer clone) {
        this.createdVariables = clone;
    }

    public void addAnnotation(ClassName annotation) {
        this.annotations.add(AnnotationSpec.builder(annotation).build());
    }

    public void addAnnotation(Class<?> annotation) {
        addAnnotation(ClassName.get(annotation));
    }

    public void addException(TypeName exception) {
        this.exceptions.add(exception);
    }

    public void addException(Type exception) {
        addException(TypeName.get(exception));
    }

    public void addModifiers(Modifier aPublic) {
        this.modifiers.add(aPublic);
    }
}
