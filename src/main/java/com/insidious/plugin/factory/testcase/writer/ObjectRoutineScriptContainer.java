package com.insidious.plugin.factory.testcase.writer;


import com.insidious.plugin.factory.testcase.TestGenerationState;
import com.insidious.plugin.factory.testcase.writer.line.CodeLine;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import lombok.AllArgsConstructor;

import java.util.LinkedList;
import java.util.List;

/**
 * A convenient container for list of ObjectRoutine. we always have one constructor routine for
 * the object, which is always named &lt;init&gt; and the other methods have proper names. Add
 * statements is redirected to most recently created object routine.
 */
@AllArgsConstructor
public class ObjectRoutineScriptContainer {

    private final static Logger logger = LoggerUtil.getInstance(ObjectRoutineScript.class);
    private final List<ObjectRoutineScript> objectRoutines = new LinkedList<>();
    private final TestGenerationState testGenerationState;
    private final TestCaseGenerationConfiguration generationConfiguration;
    private ObjectRoutineScript currentRoutine;
    private ObjectRoutineScript constructor = newRoutine("<init>");
    /**
     * Name for variable for this particular object
     */
    private String name;
    private List<Parameter> fields = new LinkedList<>();
    private String testMethodName;

    public ObjectRoutineScriptContainer(String packageName,
                                        TestGenerationState testGenerationState,
                                        TestCaseGenerationConfiguration generationConfiguration
    ) {
        this.testGenerationState = testGenerationState;
        this.generationConfiguration = generationConfiguration;
    }

    @Override
    public String toString() {
        return "ObjectRoutineScriptContainer{" +
                "objectRoutines=" + objectRoutines +
                ", testGenerationState=" + testGenerationState +
                ", currentRoutine=" + currentRoutine +
                ", constructor=" + constructor +
                ", name='" + name + '\'' +
                ", fields=" + fields +
                ", testMethodName='" + testMethodName + '\'' +
                '}';
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


    public void addField(Parameter field) {
        this.fields.add(field);
    }

    public TestGenerationState getTestGenerationState() {
        return testGenerationState;
    }

    public ObjectRoutineScript newRoutine(String routineName) {
        assert routineName != null;
        for (ObjectRoutineScript objectRoutine : this.objectRoutines) {
            if (objectRoutine.getRoutineName()
                    .equals(routineName)) {
                this.currentRoutine = objectRoutine;
                return objectRoutine;
            }
        }

        ObjectRoutineScript newRoutine = new ObjectRoutineScript(routineName, generationConfiguration);
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

    public List<Parameter> getFields() {
        return this.fields;
    }

    public String getTestMethodName() {
        return testMethodName;
    }

    public void setTestMethodName(String testMethodName) {
        this.testMethodName = testMethodName;
    }
}
