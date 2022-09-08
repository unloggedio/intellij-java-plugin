package com.insidious.plugin.factory.testcase.writer;


import com.insidious.plugin.factory.testcase.writer.line.CodeLine;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import lombok.AllArgsConstructor;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * A convenient container for list of ObjectRoutine. we always have one constructor routine for
 * the object, which is always named &lt;init&gt; and the other methods have proper names. Add
 * statements is redirected to most recently created object routine.
 */
@AllArgsConstructor
public class ObjectRoutineScriptContainer {

    private final String packageName;
    private final static Logger logger = LoggerUtil.getInstance(ObjectRoutineScript.class);
    private final List<ObjectRoutineScript> objectRoutines = new LinkedList<>();
    private List<FieldSpec> fieldSpecList = new LinkedList<>();
    private List<MethodSpec> methodSpecList = new LinkedList<>();
    private ObjectRoutineScript currentRoutine;
    private ObjectRoutineScript constructor = newRoutine("<init>");
    /**
     * Name for variable for this particular object
     */
    private String name;


    public ObjectRoutineScriptContainer(String packageName) {
        this.packageName = packageName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ObjectRoutineScript> getObjectRoutines() {
        return objectRoutines;
    }


    public void addField(FieldSpec field) {
        this.fieldSpecList.add(field);
    }

    public ObjectRoutineScript newRoutine(String routineName) {
        if (routineName == null) {
            return new ObjectRoutineScript();
        }
        for (ObjectRoutineScript objectRoutine : this.objectRoutines) {
            if (objectRoutine.getRoutineName().equals(routineName)) {
                this.currentRoutine = objectRoutine;
                return objectRoutine;
            }
        }

        ObjectRoutineScript newRoutine = new ObjectRoutineScript(routineName);
        this.objectRoutines.add(newRoutine);
        this.currentRoutine = newRoutine;
        return newRoutine;
    }

    public List<Pair<CodeLine, Object[]>> getStatements() {
        return currentRoutine.getStatements();
    }

    public ObjectRoutineScript getConstructor() {
        return constructor;
    }

    public List<MethodSpec> getMethodSpecList() {
        return this.methodSpecList;
    }

    public Collection<FieldSpec> getFieldSpecList() {
        return this.fieldSpecList;
    }
}
