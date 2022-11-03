package com.insidious.plugin.factory.testcase.routine;

import com.insidious.plugin.factory.testcase.TestGenerationState;
import com.insidious.plugin.factory.testcase.candidate.CandidateMetadataFactory;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScript;
import com.insidious.plugin.factory.testcase.writer.line.CodeLineFactory;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.ResourceEmbedMode;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
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
            VariableContainer createdVariables,
            TestCaseGenerationConfiguration generationConfiguration,
            TestGenerationState testGenerationState
    ) {
        ObjectRoutineScript scriptContainer = new ObjectRoutineScript(
                "testAs" + VariableContainer.upperInstanceName(routineName)
        );

        scriptContainer.setCreatedVariables(createdVariables.clone());

        List<ClassName> annotations = List.of(generationConfiguration.getTestAnnotationType());
        if (getRoutineName().equals("<init>")) {
            annotations = List.of(generationConfiguration.getTestBeforeAnnotationType());
        }
        annotations.forEach(scriptContainer::addAnnotation);

        scriptContainer.addException(Exception.class);
        scriptContainer.addModifiers(Modifier.PUBLIC);

        VariableContainer variableContainer = new VariableContainer();
        for (TestCandidateMetadata testCandidateMetadata : this.testCandidateList) {
            VariableContainer candidateVariables = scriptContainer.getCreatedVariables();
            candidateVariables.all().forEach(variableContainer::add);
            testGenerationState.setVariableContainer(variableContainer);
            ObjectRoutineScript script = CandidateMetadataFactory
                    .toObjectScript(testCandidateMetadata, testGenerationState, generationConfiguration);
            scriptContainer.getStatements().addAll(script.getStatements());
        }

        if (generationConfiguration.getResourceEmbedMode().equals(ResourceEmbedMode.IN_FILE)) {
            if (testGenerationState.getValueResourceMap().size() > 0) {
                scriptContainer.getStatements().add(0, Pair.create(
                        CodeLineFactory.StatementCodeLine("LoadResources(this.getClass(), $S)"), new Object[]{
                                ((MethodCallExpression) this.testCandidateList.get(this.testCandidateList.size() - 1).getMainMethod()).getMethodName()
                        }));
            }
        }

        return scriptContainer;

    }


}
