package com.insidious.plugin.factory.testcase.routine;

import com.insidious.common.weaver.ClassInfo;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.factory.testcase.TestGenerationState;
import com.insidious.plugin.factory.testcase.candidate.CandidateMetadataFactory;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.factory.testcase.mock.MockFactory;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScript;
import com.insidious.plugin.factory.testcase.writer.line.CodeLineFactory;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.ResourceEmbedMode;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.squareup.javapoet.ClassName;
import lombok.AllArgsConstructor;

import javax.lang.model.element.Modifier;
import java.util.*;
import java.util.stream.Collectors;

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
            TestGenerationState testGenerationState,
            SessionInstance sessionInstance) {
        TestCandidateMetadata lastCandidate = generationConfiguration
                .getTestCandidateMetadataList()
                .get(generationConfiguration.getTestCandidateMetadataList()
                        .size() - 1);

        MethodCallExpression lastCandidateMainMethod = (MethodCallExpression) lastCandidate.getMainMethod();
        ObjectRoutineScript scriptContainer = new ObjectRoutineScript(
                "testMethod" + StringUtil.toTitleCase(lastCandidateMainMethod.getMethodName()),
                generationConfiguration
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
        VariableContainer fieldsContainer = new VariableContainer();
        List<MethodCallExpression> callsList = new ArrayList<>();

        List<TestCandidateMetadata> mockCreatorCalls = this.testCandidateList.stream()
                .filter(e -> ((MethodCallExpression) e.getMainMethod()).getMethodName()
                        .equals("mock"))
                .collect(Collectors.toList());

        Map<String, ClassInfo> classIndex = sessionInstance.getClassIndex();
        List<Parameter> nonPojoParameters =
                this.testCandidateList.stream()
                        .map(e -> (MethodCallExpression) e.getMainMethod())
                        .map(MethodCallExpression::getArguments)
                        .flatMap(Collection::stream)
                        .filter(e -> classIndex.get(e.getType()) != null
                                && !classIndex.get(e.getType())
                                .isPojo()
                                && !classIndex.get(e.getType())
                                .isEnum()
                        )
                        .collect(Collectors.toList());

        for (Parameter nonPojoParameter : nonPojoParameters) {
            TestCandidateMetadata metadata = MockFactory.createParameterMock(nonPojoParameter, generationConfiguration);
            if (metadata == null) {
                logger.warn("unable to create a initializer for non pojo parameter: " + nonPojoParameter);
                continue;
            }
            fieldsContainer.add(nonPojoParameter);
            ObjectRoutineScript script1 = CandidateMetadataFactory
                    .mainMethodToObjectScript(metadata, testGenerationState, generationConfiguration);

            scriptContainer.getStatements()
                    .addAll(script1.getStatements());
            scriptContainer.getStaticMocks()
                    .addAll(script1.getStaticMocks());
            scriptContainer.getCreatedVariables()
                    .add(nonPojoParameter);
        }


        for (TestCandidateMetadata testCandidateMetadata : this.testCandidateList) {
            VariableContainer candidateVariables = scriptContainer.getCreatedVariables();
            candidateVariables.all()
                    .forEach(variableContainer::add);
            testGenerationState.setVariableContainer(variableContainer);

            callsList.addAll(testCandidateMetadata.getCallsList());
            testCandidateMetadata.getFields()
                    .all()
                    .forEach(fieldsContainer::add);

        }

        ObjectRoutineScript script = CandidateMetadataFactory
                .callMocksToObjectScript(testGenerationState, generationConfiguration, callsList, fieldsContainer);

        scriptContainer.getStatements()
                .addAll(script.getStatements());
        scriptContainer.getStaticMocks()
                .addAll(script.getStaticMocks());


        for (TestCandidateMetadata testCandidateMetadata : mockCreatorCalls) {
            VariableContainer candidateVariables = scriptContainer.getCreatedVariables();
            candidateVariables.all()
                    .forEach(variableContainer::add);
            testGenerationState.setVariableContainer(variableContainer);

            ObjectRoutineScript script1 = CandidateMetadataFactory
                    .mainMethodToObjectScript(testCandidateMetadata, testGenerationState, generationConfiguration);

            scriptContainer.getStatements()
                    .addAll(script1.getStatements());
            scriptContainer.getStaticMocks()
                    .addAll(script1.getStaticMocks());

        }

        for (TestCandidateMetadata testCandidateMetadata : this.testCandidateList) {
            if (mockCreatorCalls.contains(testCandidateMetadata)) {
                continue;
            }
            VariableContainer candidateVariables = scriptContainer.getCreatedVariables();
            candidateVariables.all()
                    .forEach(variableContainer::add);
            testGenerationState.setVariableContainer(variableContainer);

            ObjectRoutineScript script1 = CandidateMetadataFactory
                    .mainMethodToObjectScript(testCandidateMetadata, testGenerationState, generationConfiguration);

            scriptContainer.getStatements()
                    .addAll(script1.getStatements());
            scriptContainer.getStaticMocks()
                    .addAll(script1.getStaticMocks());

        }

        if (generationConfiguration.getResourceEmbedMode()
                .equals(ResourceEmbedMode.IN_FILE)) {
            if (testGenerationState.getValueResourceMap()
                    .size() > 0) {
                scriptContainer.getStatements()
                        .add(0, Pair.create(
                                CodeLineFactory.StatementCodeLine("LoadResources(this.getClass(), $S)"), new Object[]{
                                        ((MethodCallExpression) this.testCandidateList.get(
                                                        this.testCandidateList.size() - 1)
                                                .getMainMethod()).getMethodName()
                                }));
            }
        }

        return scriptContainer;
    }
}
