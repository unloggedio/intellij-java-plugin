package com.insidious.plugin.factory.testcase.routine;

import com.insidious.plugin.factory.testcase.candidate.CandidateMetadataFactory;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScript;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.squareup.javapoet.ClassName;
import lombok.AllArgsConstructor;

import javax.lang.model.element.Modifier;
import java.util.*;

/**
 * ObjectRoutine is representing a block of code, close to a method, containing all the
 * statements and dependent variables along with their own object routines (the whole hierarchy
 * should be available to recreate this object inside a test case)
 */
@AllArgsConstructor
public class ObjectRoutine {
    private final static Logger logger = LoggerUtil.getInstance(ObjectRoutine.class);
    private final String routineName;
    private final Map<String, ObjectRoutineContainer> dependentMap = new HashMap<>();
    private final List<ObjectRoutineContainer> dependentList = new LinkedList<>();
    private List<TestCandidateMetadata> testCandidateList = new LinkedList<>();
    private VariableContainer variableContainer = new VariableContainer();

    public ObjectRoutine(String routineName) {
        this.routineName = routineName;
    }


    public List<TestCandidateMetadata> getTestCandidateList() {
        return testCandidateList;
    }

    public void setTestCandidateList(TestCandidateMetadata newTestCaseMetadata) {
        List<TestCandidateMetadata> testCandidateMetadata = new ArrayList<>();
        testCandidateMetadata.add(newTestCaseMetadata);
        this.testCandidateList = new LinkedList<>(testCandidateMetadata);
    }

    public void addMetadata(TestCandidateMetadata newTestCaseMetadata) {
        if (this.testCandidateList == null) {
            this.testCandidateList = new LinkedList<>();
        }
        if (!this.testCandidateList.contains(newTestCaseMetadata)) {
            this.testCandidateList.add(newTestCaseMetadata);
        }
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


    public ObjectRoutineScript toObjectScript(
            VariableContainer createdVariables
    ) {
        ObjectRoutineScript scriptContainer = new ObjectRoutineScript(
                "testAs" + VariableContainer.upperInstanceName(routineName)
        );

        scriptContainer.setCreatedVariables(createdVariables.clone());

        List<ClassName> annotations = List.of(ClassName.bestGuess("org.junit.Test"));
        if (getRoutineName().equals("<init>")) {
            annotations = List.of(ClassName.bestGuess("org.junit.Before"));
        }
        annotations.forEach(scriptContainer::addAnnotation);

        scriptContainer.addException(Exception.class);
        scriptContainer.addModifiers(Modifier.PUBLIC);

        for (TestCandidateMetadata testCandidateMetadata : this.testCandidateList) {
            ObjectRoutineScript script = CandidateMetadataFactory.toObjectScript(testCandidateMetadata, scriptContainer.getCreatedVariables());
            scriptContainer.getStatements().addAll(script.getStatements());
        }

        return scriptContainer;

    }

    public VariableContainer getVariableContainer() {
        return variableContainer;
    }

    public void setVariableContainer(VariableContainer variableContainer) {
        this.variableContainer = variableContainer;
    }
}
