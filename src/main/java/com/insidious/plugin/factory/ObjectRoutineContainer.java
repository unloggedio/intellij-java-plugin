package com.insidious.plugin.factory;


import com.insidious.plugin.factory.expression.Expression;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.intellij.openapi.util.Pair;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

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

    public List<Parameter> getVariablesOfType(final String className) {


        List<Parameter> dependentImports = new LinkedList<>();
        for (ObjectRoutine objectRoutine : this.objectRoutines) {


            dependentImports = objectRoutine.getDependentList().stream().map(
                    e -> e.getVariablesOfType(className)
            ).flatMap(Collection::stream).collect(Collectors.toList());

            for (TestCandidateMetadata metadatum : objectRoutine.getMetadata()) {
                Expression mainMethod = metadatum.getMainMethod();
                if (mainMethod instanceof MethodCallExpression) {
                    MethodCallExpression mce = (MethodCallExpression) mainMethod;
                    if (mce.getSubject() != null && mce.getSubject().getType() != null && mce.getSubject().getType().startsWith(className)) {
                        dependentImports.add(mce.getSubject());
                    }
                    if (mce.getReturnValue() != null && mce.getReturnValue().getType() != null && mce.getReturnValue().getType().startsWith(className)) {
                        dependentImports.add(mce.getSubject());
                    }
                    for (Parameter parameter : mce.getArguments().all()) {
                        if (parameter.getType().startsWith(className)) {
                            dependentImports.add(parameter);
                        }
                    }

                }
            }


        }

        return dependentImports;


    }
}
