package com.insidious.plugin.factory;


import com.intellij.openapi.util.Pair;

import java.util.LinkedList;
import java.util.List;

public class ObjectRoutineContainer {
    public ObjectRoutineContainer(List<ObjectRoutine> constructorRoutine) {
        for (ObjectRoutine objectRoutine : constructorRoutine) {
            if (objectRoutine.getRoutineName().equals("<init>")) {
                this.constructor = objectRoutine;
            }
        }
        this.objectRoutines.clear();
        this.objectRoutines.addAll(constructorRoutine);
        this.currentRoutine = constructorRoutine.get(constructorRoutine.size() - 1);

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private final List<ObjectRoutine> objectRoutines = new LinkedList<>();


    private ObjectRoutine constructor = newRoutine("<init>");
    private ObjectRoutine currentRoutine;
    private String name;

    public ObjectRoutine newRoutine(String routineName) {
        ObjectRoutine newRoutine = new ObjectRoutine(routineName);
        this.objectRoutines.add(newRoutine);
        this.currentRoutine = newRoutine;
        return newRoutine;
    }

    public ObjectRoutineContainer() {
    }

    public void addStatement(String s,
                             Object... args) {
        currentRoutine.addStatement(s, args);
    }

    public List<ObjectRoutine> getObjectRoutines() {
        return objectRoutines;
    }

    public List<Pair<String, Object[]>> getStatements() {
        return currentRoutine.getStatements();
    }

    public void addComment(String s) {
        currentRoutine.addComment(s);
    }

    public ObjectRoutine getConstructor() {
        return constructor;
    }

    public void addMetadata(TestCandidateMetadata newTestCaseMetadata) {
        currentRoutine.addMetadata(newTestCaseMetadata);
    }

}
