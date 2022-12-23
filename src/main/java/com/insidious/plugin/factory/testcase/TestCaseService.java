package com.insidious.plugin.factory.testcase;

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
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.TestCaseUnit;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.squareup.javapoet.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import javax.lang.model.element.Modifier;
import java.util.Collection;
import java.util.List;
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

        typeSpecBuilder.addField(objectRoutineContainer.getTestSubject()
                .toFieldSpec()
                .build());
        for (Parameter field : testCaseScript.getFields()) {
            if (field == null) {
                continue;
            }
            typeSpecBuilder.addField(field.toFieldSpec()
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


        ClassName gsonClass = ClassName.get("com.google.gson", "Gson");


        typeSpecBuilder
                .addField(FieldSpec
                        .builder(gsonClass, "gson", Modifier.PRIVATE)
                        .initializer("new $T()", gsonClass)
                        .build()
                );


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
    public TestCaseUnit buildTestCaseUnit(TestCaseGenerationConfiguration generationConfiguration) {

//        List<TestCandidateMetadata> candidates = generationConfiguration.getTestCandidateMetadataList();
//        List<TestCandidateMetadata> loadedCandidateList = new LinkedList<>();
//        for (TestCandidateMetadata candidate : candidates) {
//            TestCandidateMetadata loadedCandidate = sessionInstance.getTestCandidateById(candidate.getEntryProbeIndex(),
//                    true);
//            loadedCandidateList.add(loadedCandidate);
//        }
//        generationConfiguration.setTestCandidateMetadataList(loadedCandidateList);


        ObjectRoutineContainer objectRoutineContainer = new ObjectRoutineContainer(generationConfiguration);

        createFieldMocks(objectRoutineContainer);

        ObjectRoutineScriptContainer testCaseScript = objectRoutineContainer.toRoutineScript();


        return buildTestUnitFromScript(objectRoutineContainer, testCaseScript);
    }

    public void createFieldMocks(
            ObjectRoutineContainer objectRoutineContainer
    ) {


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


        // gotta mock'em all
        for (Parameter fieldParameter : fields.all()) {
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
