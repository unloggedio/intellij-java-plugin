package com.insidious.plugin.factory.testcase;

import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.client.VideobugClientInterface;
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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class TestCaseService {
    private final Logger logger = LoggerUtil.getInstance(TestCaseService.class);
    private final SessionInstance sessionInstance;


    public TestCaseService(SessionInstance sessionInstance) {
        this.sessionInstance = sessionInstance;
    }

    @NotNull
    public TestCaseUnit getTestCaseUnit(TestCaseGenerationConfiguration testCaseGenerationConfiguration) {
        ObjectRoutineContainer objectRoutineContainer = new ObjectRoutineContainer(testCaseGenerationConfiguration);

        createFieldMocks(objectRoutineContainer);

        ObjectRoutineScriptContainer testCaseScript = objectRoutineContainer.toRoutineScript();


        String generatedTestClassName =
                "Test" + testCaseScript.getName() + "V";
        TypeSpec.Builder typeSpecBuilder = TypeSpec
                .classBuilder(generatedTestClassName)
                .addModifiers(
                        javax.lang.model.element.Modifier.PUBLIC,
                        javax.lang.model.element.Modifier.FINAL);

        for (Parameter field : testCaseScript.getFields()) {
            if (field == null) {
                continue;
            }
            typeSpecBuilder.addField(field.toFieldSpec().build());
        }


        for (ObjectRoutineScript objectRoutine : testCaseScript.getObjectRoutines()) {
            if (objectRoutine.getName().equals("<init>")) {
                continue;
            }
            MethodSpec methodSpec = objectRoutine.toMethodSpec().build();
            typeSpecBuilder.addMethod(methodSpec);
        }

        typeSpecBuilder.addMethod(MethodSpecUtil.createInjectFieldMethod());

        if (objectRoutineContainer.getVariablesOfType("okhttp3.").size() > 0) {
            typeSpecBuilder.addMethod(MethodSpecUtil.createOkHttpMockCreator());
        }


        ClassName gsonClass = ClassName.get("com.google.gson", "Gson");


        typeSpecBuilder
                .addField(FieldSpec
                        .builder(gsonClass,
                                "gson", javax.lang.model.element.Modifier.PRIVATE)
                        .initializer("new $T()", gsonClass)
                        .build());


        TypeSpec helloWorld = typeSpecBuilder.build();

        JavaFile javaFile = JavaFile.builder(objectRoutineContainer.getPackageName(), helloWorld)
                .addStaticImport(ClassName.bestGuess("org.mockito.ArgumentMatchers"), "*")
                .build();


        TestCaseUnit testCaseUnit = new TestCaseUnit(
                javaFile.toString(), objectRoutineContainer.getPackageName(), generatedTestClassName);
        return testCaseUnit;
    }

    public void createFieldMocks(
            ObjectRoutineContainer objectRoutineContainer
    ) {


        VariableContainer fields = VariableContainer.from(
                objectRoutineContainer
                        .getObjectRoutines().stream()
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
            TestCandidateMetadata metadata = MockFactory.createParameterMock(fieldParameter);
            constructor.addMetadata(metadata);

            if (fieldParameter.getName() != null && objectRoutineContainer.getName() == null) {
                objectRoutineContainer.setName(fieldParameter.getName());
            }
        }


    }

    public void processLogFiles() throws Exception {
        sessionInstance.scanDataAndBuildReplay();
    }


    public List<TestCandidateMetadata> getTestCandidatesForMethod(String className, String methodName, boolean loadCalls) {
        return sessionInstance.getTestCandidatesForMethod(className, methodName, loadCalls);
    }

    public @NotNull TestCaseUnit getTestCaseUnit(TestCandidateMetadata testCandidateMetadata) {

        testCandidateMetadata = sessionInstance.getTestCandidateById(testCandidateMetadata.getEntryProbeIndex());

        List<TestCandidateMetadata> list =
                sessionInstance.getTestCandidatesForMethod(
                        testCandidateMetadata.getTestSubject().getType(), "<init>", true);

        list.add(testCandidateMetadata);
        TestCaseGenerationConfiguration generationConfiguration = new TestCaseGenerationConfiguration();
        generationConfiguration.getTestCandidateMetadataList().addAll(list);
        for (TestCandidateMetadata candidateMetadata : list) {
            generationConfiguration.getCallExpressionList().addAll(candidateMetadata.getCallsList());
        }
        return getTestCaseUnit(generationConfiguration);
    }
}
