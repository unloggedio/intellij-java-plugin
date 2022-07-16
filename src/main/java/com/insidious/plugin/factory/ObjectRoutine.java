package com.insidious.plugin.factory;

import com.intellij.openapi.util.Pair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ObjectRoutine {
    private final List<Pair<String, Object[]>> statements = new LinkedList<>();
    private final String routineName;
    private final Map<String, ObjectRoutineContainer> dependentMap = new HashMap<>();
    private final List<ObjectRoutineContainer> dependentList = new LinkedList<>();

    public VariableContainer getVariableContainer() {
        return variableContainer;
    }

    public void setVariableContainer(VariableContainer variableContainer) {
        this.variableContainer = variableContainer;
    }

    private VariableContainer variableContainer = new VariableContainer();
    private List<TestCandidateMetadata> metadata = new LinkedList<>();

    public ObjectRoutine(String routineName) {
        this.routineName = routineName;
    }

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
}
