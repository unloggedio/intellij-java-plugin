package com.insidious.plugin.factory;


import com.intellij.openapi.util.Pair;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.LinkedList;
import java.util.List;

/**
 * A convenient container for list of ObjectRoutine. we always have one constructor routine for
 * the object, which is always named &lt;init&gt; and the other methods have proper names. Add
 * statements is redirected to most recently created object routine.
 */
@AllArgsConstructor
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

    /**
     * Name for variable for this particular object
     */
    private String name;

    public ObjectRoutine newRoutine(String routineName) {
        for (ObjectRoutine objectRoutine : this.objectRoutines) {
            if (objectRoutine.getRoutineName().equals(routineName)) {
                this.currentRoutine = objectRoutine;
                return objectRoutine;
            }
        }

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

    public List<Pair<CodeLine, Object[]>> getStatements() {
        return currentRoutine.getStatements();
    }

    public void addComment(String s) {
        currentRoutine.addComment(s);
    }

    public ObjectRoutine getConstructor() {
        return constructor;
    }

    public ObjectRoutine getRoutineByName(String name) {
        for (ObjectRoutine objectRoutine : this.objectRoutines) {
            if (objectRoutine.getRoutineName().equals(name)) {
                return objectRoutine;
            }
        }
        return null;

    }

    public void addMetadata(TestCandidateMetadata newTestCaseMetadata) {
        currentRoutine.addMetadata(newTestCaseMetadata);
    }
}
