package com.insidious.plugin.factory.testcase;

import com.insidious.common.weaver.TypeInfo;
import com.insidious.plugin.callbacks.GetProjectSessionsCallback;
import com.insidious.plugin.client.ParameterNameFactory;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.factory.testcase.mock.MockFactory;
import com.insidious.plugin.factory.testcase.routine.ObjectRoutine;
import com.insidious.plugin.factory.testcase.routine.ObjectRoutineContainer;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.factory.testcase.util.MethodSpecUtil;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScript;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScriptContainer;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.TestCaseUnit;
import com.insidious.plugin.ui.RefreshButtonStateManager;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.search.GlobalSearchScope;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import javax.lang.model.element.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public class TestCaseService implements Runnable {
    private static final Logger logger = LoggerUtil.getInstance(TestCaseService.class);
    private final SessionInstance sessionInstance;
    private final Project project;
    private boolean pauseCheckingForNewLogs;
    private boolean isProcessing;
    private RefreshButtonStateManager refreshButtonStateManager;

    public TestCaseService(SessionInstance sessionInstance) {
        this.sessionInstance = sessionInstance;
        this.project = sessionInstance.getProject();
//        this.sessionInstance.submitTask(this);
    }

    public void startRun() {
        this.sessionInstance.submitTask(this);
    }

    public RefreshButtonStateManager getRefreshButtonStateManager() {
        return refreshButtonStateManager;
    }

    public void setRefreshButtonStateManager(RefreshButtonStateManager refreshButtonStateManager) {
        this.refreshButtonStateManager = refreshButtonStateManager;
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

        Parameter targetTestSubject = generationConfiguration.getTestCandidateMetadataList()
                .get(0)
                .getTestSubject();

        TestCandidateMetadata constructorCandidate = sessionInstance.getConstructorCandidate(targetTestSubject);
        // this can be null for static classes
        if (constructorCandidate != null) {
            generationConfiguration.getTestCandidateMetadataList()
                    .add(0, constructorCandidate);
        }

        ObjectRoutineContainer objectRoutineContainer = new ObjectRoutineContainer(generationConfiguration);

        List<TestCandidateMetadata> mockCreatorCandidates = createFieldMocks(objectRoutineContainer);

        ObjectRoutine constructorRoutine = objectRoutineContainer.getConstructor();
        for (TestCandidateMetadata mockCreatorCandidate : mockCreatorCandidates) {
            Parameter subjectParameter = mockCreatorCandidate.getTestSubject();
            String subjectParameterType = subjectParameter.getType();
            if (subjectParameterType.startsWith("org.springframework.cglib.proxy.")) {
                continue;
            }
            if (subjectParameterType.startsWith("org.slf4j.")) {
                continue;
            }
            if (subjectParameterType.startsWith("com.google.gson.")) {
                continue;
            }
            if (subjectParameterType.startsWith("com.fasterxml.jackson.databind.")) {
                continue;
            }

            objectRoutineContainer.addFieldParameter(subjectParameter);
            constructorRoutine.addMetadata(mockCreatorCandidate);
        }

        ObjectRoutineScriptContainer testCaseScript =
                objectRoutineContainer.toObjectRoutineScriptContainer(sessionInstance, testGenerationState);


        return buildTestUnitFromScript(objectRoutineContainer, testCaseScript);
    }

    public List<TestCandidateMetadata> createFieldMocks(ObjectRoutineContainer objectRoutineContainer) {

        List<TestCandidateMetadata> mockCreatorCandidates = new ArrayList<>();

        Parameter target = objectRoutineContainer.getTestSubject();

        Set<? extends Parameter> fields = objectRoutineContainer.collectFieldsFromRoutines();


        @Nullable PsiClass classPsiInstance = null;
        try {
            classPsiInstance = JavaPsiFacade.getInstance(project)
                    .findClass(ClassTypeUtils.getJavaClassName(target.getType()), GlobalSearchScope.allScope(project));
        } catch (IndexNotReadyException e) {
            InsidiousNotification.notifyMessage("Test Generation can start only after indexing is complete!",
                    NotificationType.ERROR);
        }


        // gotta mock'em all
        for (Parameter fieldParameter : fields) {

            if (fieldParameter.getType()
                    .startsWith("org.slf4j.Logger")) {
                continue;
            }
            if (fieldParameter.getType()
                    .startsWith("org.springframework.cglib.proxy.MethodInterceptor")) {
                continue;
            }

            List<String> typeNames = new LinkedList<>();
            typeNames.add(fieldParameter.getType());

            TypeInfo fieldTypeInfo = sessionInstance.getTypeInfo(fieldParameter.getType());
            for (int interfaceTypeId : fieldTypeInfo.getInterfaces()) {
                TypeInfo interfaceTypeInfo = sessionInstance.getTypeInfo(interfaceTypeId);
                typeNames.add(interfaceTypeInfo.getTypeNameFromClass());
            }


            if (classPsiInstance != null) {
                List<PsiField> fieldMatchingParameterType = Arrays.stream(classPsiInstance.getFields())
                        .filter(e -> typeNames.contains(e.getType()
                                .getCanonicalText()))
                        .collect(Collectors.toList());
                if (fieldMatchingParameterType.size() > 0) {

                    List<PsiField> fieldMatchingNameAndType = fieldMatchingParameterType.stream()
                            .filter(e -> fieldParameter.hasName(e.getName()))
                            .collect(Collectors.toList());

                    boolean nameChosen = false;
                    if (fieldMatchingNameAndType.size() == 0) {
                        logger.warn("no matching field of type [" + fieldParameter.getType()
                                + "] with matching [" + fieldParameter.getNames() + "] was found. The names found were: "
                                + fieldMatchingParameterType.stream()
                                .map(PsiField::getName)
                                .collect(Collectors.toList()));
                    } else if (fieldMatchingNameAndType.size() > 1) {
                        logger.warn("more than 1 matching field of type [" + fieldParameter.getType()
                                + "] with matching [" + fieldParameter.getNames() + "] was found. The names found were: "
                                + fieldMatchingParameterType.stream()
                                .map(PsiField::getName)
                                .collect(Collectors.toList()));
                    } else {
                        nameChosen = true;
                        fieldParameter.getNames().clear();
                        fieldParameter.setName(fieldMatchingNameAndType.get(0).getName());
                    }
                    if (!nameChosen && fieldMatchingParameterType.size() == 1) {
                        // if we didn't find a field with matching name
                        // but we have only 1 field with matching type, then we will use the name of that field
                        fieldParameter.getNames()
                                .clear();
                        fieldParameter.setName(fieldMatchingParameterType.get(0)
                                .getName());
                    }

                } else {
                    logger.warn(
                            "no matching field of type [" + fieldParameter.getType() + "] found in class [" + target.getType() + "]");
                }
            }
        }

        for (Parameter fieldParameter : fields) {
            TestCandidateMetadata metadata = MockFactory.createParameterMock(fieldParameter,
                    objectRoutineContainer.getGenerationConfiguration());
            if (metadata == null) {
                logger.warn("unable to create a initializer for field: " + fieldParameter);
                continue;
            }
            mockCreatorCandidates.add(metadata);
        }


        return mockCreatorCandidates;

    }

    public void processLogFiles() throws Exception {
        if (isProcessing) {
            return;
        }
        isProcessing = true;
        long startTime = new Date().getTime();
        sessionInstance.scanDataAndBuildReplay();
        long endTime = new Date().getTime();
        logger.warn("Scan took: " + (endTime - startTime) + " ms");
        isProcessing = false;
    }

    public List<TestCandidateMetadata> getTestCandidatesForMethod(String className, String methodName, boolean loadCalls) {
        return sessionInstance.getTestCandidatesForMethod(className, methodName, loadCalls);
    }

    //    @Override
    public void run_old() {
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

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(2000);
                if (this.refreshButtonStateManager.isProcessing()) {
                    continue;
                }
                if(hasNewSession())
                {
                    this.refreshButtonStateManager.setState_NewSession();
                }
                else
                {
                    if (sessionInstance.hasNewZips()) {
                        //set state to new logs
                        this.refreshButtonStateManager.setState_NewLogs(sessionInstance.getLastScannedTimeStamp());
                    } else {
                        //set state to no new logs
                        this.refreshButtonStateManager.setState_NoNewLogs(sessionInstance.getLastScannedTimeStamp());
                    }
                }
            } catch (InterruptedException ex) {
                logger.warn("Thread interrupted scanning a new session.", ex);
                throw new RuntimeException(ex);
            } catch (Exception e) {
                logger.error("Exception in testcase service, scanner shutting down", e);
                throw new RuntimeException(e);
            }
        }
    }

    private boolean hasNewSession() {
        final boolean[] hasNew = {false};
        project.getService(InsidiousService.class).getClient()
                .getProjectSessions(new GetProjectSessionsCallback() {
                    @Override
                    public void error(String message) {
                        hasNew[0] =false;
                    }

                    @Override
                    public void success(List<ExecutionSession> executionSessionList) {
                        if(executionSessionList.size()==0)
                        {
                            hasNew[0] = false;
                        }
                        else
                        {
//                            System.out.println("Exec Current ID "+executionSessionList.get(0).getSessionId() +
//                                    " Created : "+executionSessionList.get(0).getCreatedAt());
//                            System.out.println("Current Session ID : "+sessionInstance.getExecutionSession().getSessionId() +
//                                    " Created session : "+sessionInstance.getExecutionSession().getCreatedAt());
                            if(!executionSessionList.get(0).getSessionId().equals(sessionInstance.getExecutionSession().getSessionId()))
                            {
                                if(executionSessionList.get(0).getCreatedAt().compareTo(sessionInstance.getExecutionSession().getCreatedAt())>0)
                                {
                                    hasNew[0]=true;
                                }
                                else if(executionSessionList.size()==1)
                                {
                                    hasNew[0]=true;
                                }
                            }
                        }
                    }
                });
        System.out.println("Has new sessions check : "+hasNew[0]);
        return hasNew[0];
    }
}
