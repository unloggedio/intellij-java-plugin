package com.insidious.plugin.factory;


import com.insidious.plugin.pojo.SessionCache;
import com.intellij.openapi.util.Pair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ObjectRoutineContainer {

    private final List<ObjectRoutine> objectRoutines = new LinkedList<>();


    private ObjectRoutine constructor = newRoutine("<init>");
    private ObjectRoutine currentRoutine;
    private final Map<String, ObjectRoutineContainer> dependentMap = new HashMap<>();

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

    public void addDependent(String name, ObjectRoutineContainer dependentObjectCreation) {
        this.dependentMap.put(name, dependentObjectCreation);
    }
}
