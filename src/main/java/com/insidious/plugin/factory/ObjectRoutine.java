package com.insidious.plugin.factory;

import com.insidious.plugin.pojo.Parameter;
import com.intellij.openapi.util.Pair;
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
@AllArgsConstructor
public class ObjectRoutine {
    private final List<Pair<CodeLine, Object[]>> statements = new LinkedList<>();
    private final String routineName;
    private final Map<String, ObjectRoutineContainer> dependentMap = new HashMap<>();
    private final List<ObjectRoutineContainer> dependentList = new LinkedList<>();

    public VariableContainer getVariableContainer() {
        return variableContainer;
    }

    public void setVariableContainer(VariableContainer variableContainer) {
        this.variableContainer = variableContainer;
    }

    public ObjectRoutine() {
        routineName = "<init>";
    }

    private VariableContainer variableContainer = new VariableContainer();
    private VariableContainer createdVariables = new VariableContainer();
    private List<TestCandidateMetadata> metadata = new LinkedList<>();

    public ObjectRoutine(String routineName) {
        this.routineName = routineName;
    }

    public void addStatement(String s,
                             Object... args) {
        statements.add(Pair.create(new StatementCodeLine(s), args));
    }

    public void addStatement(String s,
                             List<?> args) {
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

    public void setMetadata(TestCandidateMetadata metadata) {
        this.metadata = new LinkedList<>(List.of(metadata));
    }

    public List<TestCandidateMetadata> getMetadata() {
        return metadata;
    }

    public void addMetadata(TestCandidateMetadata newTestCaseMetadata) {
        this.metadata.add(newTestCaseMetadata);
    }

    public String getRoutineName() {
        return routineName;
    }

    public List<ObjectRoutineContainer> getDependentList() {
        return dependentList;
    }


    public void addDependent(ObjectRoutineContainer dependentObjectCreation) {
        if (this.dependentMap.containsKey(dependentObjectCreation.getName())) {
            // throw new RuntimeException("dependent already exists");
            return;
        }
        this.dependentList.add(dependentObjectCreation);
        this.dependentMap.put(dependentObjectCreation.getName(), dependentObjectCreation);
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
}
