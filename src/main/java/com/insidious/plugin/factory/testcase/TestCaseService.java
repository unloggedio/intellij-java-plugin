package com.insidious.plugin.factory.testcase;

import com.insidious.plugin.client.ParameterNameFactory;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.factory.testcase.mock.MockFactory;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.routine.ObjectRoutine;
import com.insidious.plugin.factory.testcase.routine.ObjectRoutineContainer;
import com.insidious.plugin.factory.testcase.util.MethodSpecUtil;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScript;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScriptContainer;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.TestCaseUnit;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TestCaseService implements Runnable {
    private static final Logger logger = LoggerUtil.getInstance(TestCaseService.class);
    private final SessionInstance sessionInstance;
    private boolean pauseCheckingForNewLogs;
    private boolean isProcessing;


    public TestCaseService(SessionInstance sessionInstance) {
        this.sessionInstance = sessionInstance;
//        this.sessionInstance.submitTask(this);
    }

    @NotNull
    private static TestCaseUnit buildTestUnitFromScript(
            ObjectRoutineContainer objectRoutineContainer,
            ObjectRoutineScriptContainer testCaseScript
    ) {
        String generatedTestClassName = "Test" + testCaseScript.getName() + "V";

        TypeSpec.Builder typeSpecBuilder = TypeSpec
                .classBuilder(generatedTestClassName)
                .addModifiers(
                        Modifier.PUBLIC,
                        Modifier.FINAL
                );


        typeSpecBuilder.addField(testCaseScript.getTestGenerationState()
                .toFieldSpec(objectRoutineContainer.getTestSubject())
                .build());
        for (Parameter field : testCaseScript.getFields()) {
            if (field == null) {
                continue;
            }

            typeSpecBuilder.addField(testCaseScript.getTestGenerationState()
                    .toFieldSpec(field)
                    .build());
        }


        logger.info("Convert test case script to methods with: " + testCaseScript.getObjectRoutines()
                .size());
        logger.info("Test case script: " + testCaseScript);
        for (ObjectRoutineScript objectRoutine : testCaseScript.getObjectRoutines()) {
            if (objectRoutine.getName()
                    .equalsIgnoreCase("<init>")) {
                continue;
            }
            MethodSpec methodSpec = objectRoutine.toMethodSpec()
                    .build();
            typeSpecBuilder.addMethod(methodSpec);
        }

//        typeSpecBuilder.addMethod(MethodSpecUtil.createInjectFieldMethod());

        if (objectRoutineContainer.getVariablesOfType("okhttp3.")
                .size() > 0) {
            typeSpecBuilder.addMethod(MethodSpecUtil.createOkHttpMockCreator());
        }


        TypeSpec testClassSpec = typeSpecBuilder.build();

        JavaFile javaFile = JavaFile.builder(objectRoutineContainer.getPackageName(), testClassSpec)
                .addStaticImport(ClassName.bestGuess("org.mockito.ArgumentMatchers"), "*")
                .addStaticImport(ClassName.bestGuess("io.unlogged.UnloggedTestUtils"), "*")
                .build();

        try {
            JSONObject eventProperties = new JSONObject();
            int number_of_lines = testCaseScript.getObjectRoutines()
                    .get(testCaseScript.getObjectRoutines()
                            .size() - 1)
                    .getStatements()
                    .size();

            eventProperties.put("test_case_lines", number_of_lines);
            if (number_of_lines <= 1) {
                UsageInsightTracker.getInstance()
                        .RecordEvent("EmptyTestCaseGenerated", eventProperties);
            } else {
                UsageInsightTracker.getInstance()
                        .RecordEvent("TestCaseGenerated", null);
                UsageInsightTracker.getInstance()
                        .RecordEvent("TestCaseSize", eventProperties);
            }
        } catch (Exception e) {
            System.out.println("Failed to record number of lines. Statements length : " +
                    testCaseScript.getObjectRoutines()
                            .get(testCaseScript.getObjectRoutines()
                                    .size() - 1)
                            .getStatements()
                            .size());
        }

        TestCaseUnit testCaseUnit = new TestCaseUnit(javaFile.toString(),
                objectRoutineContainer.getPackageName(),
                generatedTestClassName, testCaseScript.getTestMethodName(), testCaseScript.getTestGenerationState(),
                testClassSpec);
        return testCaseUnit;
    }

    @NotNull
    public TestCaseUnit buildTestCaseUnit(TestCaseGenerationConfiguration generationConfiguration) throws Exception {

        ParameterNameFactory parameterNameFactory = new ParameterNameFactory();
        TestGenerationState testGenerationState = new TestGenerationState(parameterNameFactory);

        Parameter target = generationConfiguration.getTestCandidateMetadataList()
                .get(0)
                .getTestSubject();

        TestCandidateMetadata constructorCandidate = sessionInstance.getConstructorCandidate(target);
        generationConfiguration.getTestCandidateMetadataList()
                .add(0, constructorCandidate);

        generationConfiguration.getCallExpressionList().addAll(generationConfiguration.getCallExpressionList());

        ObjectRoutineContainer objectRoutineContainer = new ObjectRoutineContainer(generationConfiguration);

        createFieldMocks(objectRoutineContainer);

        ObjectRoutineScriptContainer testCaseScript = objectRoutineContainer.toRoutineScript(sessionInstance,
                testGenerationState);


        return buildTestUnitFromScript(objectRoutineContainer, testCaseScript);
    }

    public void createFieldMocks(ObjectRoutineContainer objectRoutineContainer) {


        VariableContainer fields = VariableContainer.from(
                objectRoutineContainer
                        .getObjectRoutines()
                        .stream()
                        .map(ObjectRoutine::getTestCandidateList)
                        .flatMap(Collection::stream)
                        .map(TestCandidateMetadata::getFields)
                        .map(VariableContainer::all)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList())
        );


        ObjectRoutine constructor = objectRoutineContainer.getConstructor();
        List<Parameter> usedFields = new ArrayList<>();


        // gotta mock'em all
        for (Parameter fieldParameter : fields.all()) {

            Optional<MethodCallExpression> foundUsage = objectRoutineContainer
                    .getObjectRoutines()
                    .stream()
                    .map(ObjectRoutine::getTestCandidateList)
                    .flatMap(Collection::stream)
                    .map(TestCandidateMetadata::getCallsList)
                    .flatMap(Collection::stream)
                    .filter(e -> e.getSubject()
                            .getValue() == fieldParameter.getValue())
                    .findAny();
            if (foundUsage.isEmpty()) {
                //field is not actually used
                continue;
            }


            TestCandidateMetadata metadata = MockFactory.createParameterMock(fieldParameter,
                    objectRoutineContainer.getGenerationConfiguration());
            if (metadata == null) {
                logger.warn("unable to create a initializer for field: " + fieldParameter);
                continue;
            }
            constructor.addMetadata(metadata);

            if (fieldParameter.getName() != null && objectRoutineContainer.getName() == null) {
                objectRoutineContainer.setName(fieldParameter.getName());
            }
        }


    }

    public void processLogFiles() throws Exception {
        if (isProcessing) {
            return;
        }
        isProcessing = true;
        sessionInstance.scanDataAndBuildReplay();
        isProcessing = false;
    }

    public List<TestCandidateMetadata> getTestCandidatesForMethod(String className, String methodName, boolean loadCalls) {
        return sessionInstance.getTestCandidatesForMethod(className, methodName, loadCalls);
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(2000);
                if (this.pauseCheckingForNewLogs) {
                    continue;
                }
                processLogFiles();
            } catch (InterruptedException e) {
                logger.warn("test case service scanner shutting down", e);
                throw new RuntimeException(e);
            } catch (Exception e) {
                logger.error("exception in testcase service scanner shutting down", e);
                throw new RuntimeException(e);
            }
        }
    }

    public boolean isPauseCheckingForNewLogs() {
        return pauseCheckingForNewLogs;
    }

    public void setPauseCheckingForNewLogs(boolean pauseCheckingForNewLogs) {
        this.pauseCheckingForNewLogs = pauseCheckingForNewLogs;
    }
}
