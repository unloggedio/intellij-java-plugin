package com.insidious.plugin.factory.testcase;

import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.TypeInfo;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.client.ParameterNameFactory;
import com.insidious.plugin.client.SessionInstanceInterface;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.factory.testcase.mock.MockFactory;
import com.insidious.plugin.factory.testcase.routine.ObjectRoutine;
import com.insidious.plugin.factory.testcase.routine.ObjectRoutineContainer;
import com.insidious.plugin.factory.testcase.util.MethodSpecUtil;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScript;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScriptContainer;
import com.insidious.plugin.factory.testcase.writer.TestCaseWriter;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.ResourceEmbedMode;
import com.insidious.plugin.pojo.TestCaseUnit;
import com.insidious.plugin.pojo.frameworks.JsonFramework;
import com.insidious.plugin.pojo.frameworks.MockFramework;
import com.insidious.plugin.pojo.frameworks.TestFramework;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.insidious.plugin.util.ClassTypeUtils;
import com.insidious.plugin.util.ClassUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.engine.JVMNameUtil;
import com.intellij.lang.jvm.JvmMethod;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.lang.jvm.types.JvmType;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.squareup.javapoet.*;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.objectweb.asm.Opcodes;

import javax.lang.model.element.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public class TestCaseService {
    private static final Logger logger = LoggerUtil.getInstance(TestCaseService.class);
    private final SessionInstanceInterface sessionInstance;
    private final Project project;

    public TestCaseService(SessionInstanceInterface sessionInstance, Project project) {
        this.sessionInstance = sessionInstance;
        this.project = project;
    }


    private static TestCaseUnit
    buildTestUnitFromScript(ObjectRoutineContainer objectRoutineContainer, ObjectRoutineScriptContainer testCaseScript) {
        String generatedTestClassName = "Test" + testCaseScript.getName() + "V";

        TestCaseGenerationConfiguration testGenerationConfig = objectRoutineContainer.getGenerationConfiguration();
        TypeSpec.Builder testClassSpecBuilder = TypeSpec.classBuilder(generatedTestClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        if (testGenerationConfig.useMockitoAnnotations()
                && testGenerationConfig.getMockFramework().equals(MockFramework.Mockito)
        ) {
            if (testGenerationConfig.getTestFramework().equals(TestFramework.JUnit4)) {
                AnnotationSpec.Builder annotationSpec = AnnotationSpec.builder(
                        ClassName.bestGuess("org.junit.runner.RunWith"));
                annotationSpec.addMember("value", "$T.class",
                        ClassName.bestGuess("org.mockito.junit.MockitoJUnitRunner"));
                testClassSpecBuilder.addAnnotation(annotationSpec.build());
            } else if (testGenerationConfig.getTestFramework().equals(TestFramework.JUnit5)) {
                AnnotationSpec.Builder annotationSpec = AnnotationSpec.builder(
                        ClassName.bestGuess("org.junit.jupiter.api.extension.ExtendWith"));
                annotationSpec.addMember("value", "$T.class",
                        ClassName.bestGuess("org.mockito.junit.jupiter.MockitoExtension"));
                testClassSpecBuilder.addAnnotation(annotationSpec.build());
            }
        }


        FieldSpec.Builder testTargetSubjectField = testCaseScript
                .getTestGenerationState()
                .toFieldSpec(objectRoutineContainer.getTestSubject());
        if (testGenerationConfig.useMockitoAnnotations()) {
            testTargetSubjectField.addAnnotation(ClassName.bestGuess("org.mockito.InjectMocks"));
        }

        testClassSpecBuilder.addField(testTargetSubjectField.build());

        for (Parameter field : testCaseScript.getFields()) {
            if (field == null) {
                continue;
            }

            FieldSpec.Builder fieldBuilder = testCaseScript.getTestGenerationState().toFieldSpec(field);

            if (testGenerationConfig.useMockitoAnnotations()) {
                fieldBuilder.addAnnotation(ClassName.bestGuess("org.mockito.Mock"));
            }

            testClassSpecBuilder.addField(fieldBuilder.build());
        }
        if (testGenerationConfig.getResourceEmbedMode().equals(ResourceEmbedMode.IN_CODE)) {

            JsonFramework jsonFramework = testGenerationConfig.getJsonFramework();
            ClassName jsonMapperClassName = ClassName.bestGuess(jsonFramework.getInstance().getType());
            FieldSpec.Builder jsonMapperField = FieldSpec.builder(jsonMapperClassName,
                    jsonFramework.getInstance().getName(),
                    Modifier.PRIVATE);
            jsonMapperField.initializer("new $T()", jsonMapperClassName);
            testClassSpecBuilder.addField(jsonMapperField.build());


        }


        logger.info("Convert test case script to methods with: " + testCaseScript.getObjectRoutines().size());
//        logger.info("Test case script: " + testCaseScript);
        for (ObjectRoutineScript objectRoutine : testCaseScript.getObjectRoutines()) {
            if (objectRoutine.getName().equalsIgnoreCase("<init>")) {
                continue;
            }
            MethodSpec methodSpec = objectRoutine.toMethodSpec().build();
            testClassSpecBuilder.addMethod(methodSpec);
        }


        if (!objectRoutineContainer.getVariablesOfType("okhttp3.").isEmpty()) {
            testClassSpecBuilder.addMethod(MethodSpecUtil.createOkHttpMockCreator());
        }


        TypeSpec testClassSpec = testClassSpecBuilder.build();

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
                UsageInsightTracker.getInstance().RecordEvent("EmptyTestCaseGenerated", eventProperties);
            } else {
                UsageInsightTracker.getInstance().RecordEvent("TestCaseSize", eventProperties);
            }
        } catch (Exception e) {
            System.out.println("Failed to record number of lines. Statements length : " +
                    testCaseScript.getObjectRoutines()
                            .get(testCaseScript.getObjectRoutines().size() - 1)
                            .getStatements()
                            .size());
        }

        return new TestCaseUnit(javaFile.toString(),
                objectRoutineContainer.getPackageName(),
                generatedTestClassName,
                testCaseScript.getTestMethodName(),
                testCaseScript.getTestGenerationState(),
                testClassSpec,
                testGenerationConfig
        );
    }

    public static void normalizeMethodTypes(MethodCallExpression mainMethod, PsiMethod targetMethodPsi, PsiSubstitutor substitutor) {
        JvmMethod selectedPsiMethod = targetMethodPsi;

        // fix argument types
        JvmParameter[] methodParameters = selectedPsiMethod.getParameters();
        List<Parameter> methodArguments = mainMethod.getArguments();
        for (int i = 0; i < methodParameters.length; i++) {
            JvmParameter parameter = methodParameters[i];
            Parameter ourParam = methodArguments.get(i);
            ourParam.addName(parameter.getName());
            PsiType type = ClassTypeUtils.substituteClassRecursively((PsiType) parameter.getType(), substitutor);
            TestCaseWriter.setParameterTypeFromPsiType(ourParam, type, false);
        }

        JvmType returnType = selectedPsiMethod.getReturnType();
        if (returnType != null) {
            if (returnType instanceof PsiType) {
                JvmType finalReturnType = returnType;
                returnType = ClassTypeUtils.
                        substituteClassRecursively((PsiType) finalReturnType, substitutor);
            }
            TestCaseWriter.setParameterTypeFromPsiType(mainMethod.getReturnValue(),
                    (PsiType) returnType, true);
        }
        if (mainMethod.getMethodName().equals("<init>")) {
            TestCaseWriter.setParameterTypeFromPsiType(mainMethod.getReturnValue(),
                    PsiTypesUtil.getClassType(targetMethodPsi.getContainingClass()), true);
        }
    }

    public static void normalizeMethodTypes(MethodCallExpression mainMethod,
                                            PsiMethodCallExpression psiCallExpression) {
        @Nullable PsiMethod targetMethodPsi = psiCallExpression.resolveMethod();
        if (targetMethodPsi == null) {
            return;
        }
        PsiReferenceExpression methodExpression = psiCallExpression.getMethodExpression();
        PsiType fieldType = methodExpression.getQualifierExpression().getType();
        if (fieldType == null) {
            return;
        }
        PsiSubstitutor classSubstitutor = ClassUtils.getSubstitutorForCallExpression(psiCallExpression);

        // fix argument types
        JvmParameter[] methodParameters = ((JvmMethod) targetMethodPsi).getParameters();
        List<Parameter> methodArguments = mainMethod.getArguments();
        for (int i = 0; i < methodParameters.length; i++) {
            JvmParameter parameter = methodParameters[i];
            Parameter ourParam = methodArguments.get(i);
            ourParam.addName(parameter.getName());
            PsiType type = ClassTypeUtils.substituteClassRecursively((PsiType) parameter.getType(), classSubstitutor);
            TestCaseWriter.setParameterTypeFromPsiType(ourParam, type, false);
        }

        if (((JvmMethod) targetMethodPsi).getReturnType() != null) {
            PsiType returnType = (PsiType) ((JvmMethod) targetMethodPsi).getReturnType();
            PsiType ungenericType = ClassTypeUtils.substituteClassRecursively(returnType, classSubstitutor);
            TestCaseWriter.setParameterTypeFromPsiType(mainMethod.getReturnValue(),
                    ungenericType, true);
        }
    }


    public TestCaseUnit buildTestCaseUnit(TestCaseGenerationConfiguration generationConfiguration) throws Exception {


        ParameterNameFactory parameterNameFactory = new ParameterNameFactory();
        TestGenerationState testGenerationState = new TestGenerationState(parameterNameFactory);

        TestCandidateMetadata testCandidateMetadata1 = generationConfiguration
                .getTestCandidateMetadataList()
                .get(0);
        MethodCallExpression targetMethod = testCandidateMetadata1.getMainMethod();
        String targetMethodName = targetMethod.getMethodName();
        Parameter targetTestSubject = testCandidateMetadata1.getTestSubject();
        TestCaseWriter testCaseWriter = new TestCaseWriter();

        List<TestCandidateMetadata> boilerplatedCandidates = new ArrayList<>();
        for (TestCandidateMetadata testCandidateMetadata : generationConfiguration.getTestCandidateMetadataList()) {
            MethodCallExpression mainMethod = testCandidateMetadata.getMainMethod();

            Pair<PsiMethod, PsiSubstitutor> targetMethodPsiPair = ClassTypeUtils.getPsiMethod(
                    mainMethod, project);
            PsiMethod targetMethodPsi = null;
            if (targetMethodPsiPair == null) {
                continue;
            }
            targetMethodPsi = targetMethodPsiPair.getFirst();
            PsiSubstitutor psiSubstitutor = targetMethodPsiPair.getSecond();
            List<TestCandidateMetadata> candidates = testCaseWriter.generateTestCaseBoilerPlace(
                    new JavaMethodAdapter(targetMethodPsi), generationConfiguration);

            boilerplatedCandidates.addAll(candidates);
//            for (TestCandidateMetadata tcm : candidates) {
//                // mock all calls by default
//                generationConfiguration.getCallExpressionList()
//                        .addAll(tcm.getCallsList());
//            }

        }

        boolean hasConstructor = false;
        for (TestCandidateMetadata boilerplatedCandidate : boilerplatedCandidates) {
            if (boilerplatedCandidate.getMainMethod().isConstructor()) {
                generationConfiguration.getTestCandidateMetadataList().add(0, boilerplatedCandidate);
                if (boilerplatedCandidate.getMainMethod().getSubject().getType().equals(targetTestSubject.getType())) {
                    boilerplatedCandidate.getMainMethod().setSubject(targetTestSubject);
                    hasConstructor = true;
                }
            }
        }


        Optional<TestCandidateMetadata> bbt = boilerplatedCandidates.stream().filter(
                e -> e.getMainMethod().getMethodName().equals(targetMethodName) &&
                        e.getMainMethod().getSubject().getType().equals(targetMethod.getSubject().getType())
        ).findFirst();

        if (bbt.isPresent()) {
//            generationConfiguration.getTestCandidateMetadataList().clear();
//            generationConfiguration.getTestCandidateMetadataList().add(bbt.get());
        }


        if (!hasConstructor) {
            TestCandidateMetadata constructorCandidate = sessionInstance.getConstructorCandidate(targetTestSubject);
            // this can be null for static classes
            if (constructorCandidate != null) {
                generationConfiguration.getTestCandidateMetadataList().add(0, constructorCandidate);
            }

        }
        ApplicationManager.getApplication().runReadAction(() -> {
            normalizeTypeInformationUsingProject(generationConfiguration);
        });

        ObjectRoutineContainer objectRoutineContainer = new ObjectRoutineContainer(generationConfiguration);

        List<TestCandidateMetadata> mockCreatorCandidates = ApplicationManager.getApplication().runReadAction(
                (Computable<List<TestCandidateMetadata>>) () -> createFieldMocks(objectRoutineContainer));

        ObjectRoutine constructorRoutine = objectRoutineContainer.getConstructor();
        for (TestCandidateMetadata mockCreatorCandidate : mockCreatorCandidates) {
            Parameter subjectParameter = mockCreatorCandidate.getTestSubject();
            if (subjectParameter == null) {
                logger.warn("subject parameter is null for mock creator candidate: " + mockCreatorCandidate);
                continue;
            }
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

    private void normalizeTypeInformationUsingProject(TestCaseGenerationConfiguration generationConfiguration) {

        List<TestCandidateMetadata> testCandidateMetadataList = new ArrayList<>(
                generationConfiguration.getTestCandidateMetadataList());
        for (TestCandidateMetadata testCandidateMetadata : testCandidateMetadataList) {


            MethodCallExpression mainMethod = testCandidateMetadata.getMainMethod();

            Pair<PsiMethod, PsiSubstitutor> targetMethodPsiPair = ClassTypeUtils.getPsiMethod(
                    mainMethod, project);
            PsiMethod targetMethodPsi = null;
            if (targetMethodPsiPair == null) {
                continue;
            }
            targetMethodPsi = targetMethodPsiPair.getFirst();
            PsiSubstitutor psiSubstitutor = targetMethodPsiPair.getSecond();

            JavaMethodAdapter methodAdapter1 = new JavaMethodAdapter(targetMethodPsi);
            String testMethodName = "testMethod" + ClassTypeUtils.upperInstanceName(methodAdapter1.getName());
            generationConfiguration.setTestMethodName(testMethodName);


            normalizeMethodTypes(mainMethod, targetMethodPsi, psiSubstitutor);

            Collection<PsiMethodCallExpression> childCallExpressions = PsiTreeUtil.findChildrenOfType(targetMethodPsi,
                    PsiMethodCallExpression.class);

            for (MethodCallExpression methodCallExpression : testCandidateMetadata.getCallsList()) {

                PsiMethodCallExpression callExpress = null;
                for (PsiMethodCallExpression childCallExpression : childCallExpressions) {
                    String callExpressionText = childCallExpression.getText();
                    String expectedSubjectName = methodCallExpression.getSubject().getName();
                    if (callExpressionText.contains(methodCallExpression.getMethodName())
                            && (expectedSubjectName != null && callExpressionText.contains(expectedSubjectName))) {
                        callExpress = childCallExpression;
                        break;
                    }
                }
                if (callExpress == null
                ) {
                    if (targetMethodPsi.getName().equals(methodCallExpression.getMethodName())
                            && targetMethodPsi.getContainingClass().getQualifiedName()
                            .equals(methodCallExpression.getSubject().getType())) {
                        normalizeMethodTypes(methodCallExpression, targetMethodPsi, psiSubstitutor);
                    }
                } else {
                    normalizeMethodTypes(methodCallExpression, callExpress);
                }
            }


            List<Parameter> expectedFields = testCandidateMetadata.getFields().all();
//            PsiField[] actualFields = targetClass.getAllFields();
            for (Parameter fieldParameter : expectedFields) {
//                PsiField fieldAccessed = findFieldAccessedInMethodAtLine(
//                        targetMethodPsi, fieldParameter.getProbeInfo().getLine(), targetMethodPsi.getProject()
//                );
                DataInfo probeInfo = fieldParameter.getProbeInfo();
                if (probeInfo.getAttributes() == null) {
                    continue;
                }
                String nameFromProbe = probeInfo.getAttribute("Name",
                        probeInfo.getAttribute("FieldName", null));
                if (nameFromProbe != null) {
                    fieldParameter.setName(nameFromProbe);
                }
//                if (fieldAccessed != null) {
//                    fieldParameter.setName(fieldAccessed.getName());
//                    PsiType type = fieldAccessed.getType();
//                    PsiType typeSubstituted = ClassTypeUtils.substituteClassRecursively((PsiType) type,
//                            psiSubstitutor);
//
//                    TestCaseWriter.setParameterTypeFromPsiType(fieldParameter, typeSubstituted, false);
//                }
            }


        }

    }

    public List<TestCandidateMetadata> createFieldMocks(ObjectRoutineContainer objectRoutineContainer) {

        List<TestCandidateMetadata> mockCreatorCandidates = new ArrayList<>();

        Parameter target = objectRoutineContainer.getTestSubject();

        Set<? extends Parameter> fields = objectRoutineContainer.collectFieldsFromRoutines();


        PsiClass classPsiInstance = null;
        try {
            classPsiInstance = JavaPsiFacade.getInstance(project)
                    .findClass(ClassTypeUtils.getDescriptorToDottedClassName(target.getType()),
                            GlobalSearchScope.allScope(project));
        } catch (IndexNotReadyException e) {
            InsidiousNotification.notifyMessage("Test Generation can start only after indexing is complete!",
                    NotificationType.ERROR);
        }


        // gotta mock'em all
        for (Parameter fieldParameter : fields) {

            String fieldParameterType = fieldParameter.getType();
            boolean isPrimitive = fieldParameter.isPrimitiveType();
            if (fieldParameterType.startsWith("org.slf4j.Logger")) {
                continue;
            }
            if (fieldParameterType.startsWith("org.springframework.cglib.proxy.MethodInterceptor")) {
                continue;
            }

            List<String> typeNames = new LinkedList<>();
            typeNames.add(fieldParameterType);

            TypeInfo fieldTypeInfo = sessionInstance.getTypeInfo(fieldParameterType);
            int[] interfaces = fieldTypeInfo.getInterfaces();
            if (interfaces != null) {
                for (int interfaceTypeId : interfaces) {
                    TypeInfo interfaceTypeInfo = sessionInstance.getTypeInfo(interfaceTypeId);
                    typeNames.add(interfaceTypeInfo.getTypeNameFromClass());
                }
            }


            if (classPsiInstance != null) {
                List<PsiField> fieldMatchingParameterType = Arrays.stream(classPsiInstance.getFields())
                        .filter(e -> {
                            if (e.getType() instanceof PsiClassReferenceType) {
                                PsiClassReferenceType classType = (PsiClassReferenceType) e.getType();
                                return typeNames.contains(classType.rawType().getCanonicalText());
                            } else {
                                String jvmPrimitiveSignature = JVMNameUtil.getPrimitiveSignature(
                                        e.getType().getCanonicalText());
                                return typeNames.contains(e.getType().getCanonicalText()) ||
                                        (isPrimitive && typeNames.contains(jvmPrimitiveSignature)) ||
                                        (isPrimitive
                                                && typeNames.contains("J")
                                                && jvmPrimitiveSignature.equals("I"));
                            }
                        })
                        .collect(Collectors.toList());
                if (fieldMatchingParameterType.size() > 0) {

                    List<PsiField> fieldMatchingNameAndType = fieldMatchingParameterType.stream()
                            .filter(e -> fieldParameter.getName() == null || fieldParameter.hasName(e.getName()))
                            .collect(Collectors.toList());

                    boolean nameChosen = false;
                    if (fieldMatchingNameAndType.size() == 0) {
                        logger.warn("no matching field of type [" + fieldParameterType
                                + "] with matching [" + fieldParameter.getNames() + "] was found. The names found were: "
                                + fieldMatchingParameterType.stream()
                                .map(PsiField::getName)
                                .collect(Collectors.toList()));
                    } else if (fieldMatchingNameAndType.size() > 1) {
                        logger.warn("more than 1 matching field of type [" + fieldParameterType
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
                        // if we didn't find a field with matching name,
                        // but we have only 1 field with matching type, then we will use the name of that field
                        fieldParameter.getNames().clear();
                        fieldParameter.setName(fieldMatchingParameterType.get(0).getName());
                    }

                } else {
                    logger.warn(
                            "no matching field of type [" + fieldParameterType + "] found in class [" + target.getType() + "]");
                }
            }
        }

        for (Parameter fieldParameter : fields) {
            if (fieldParameter.isPrimitiveType()) {

                switch (fieldParameter.getType()) {
                    case "I":
                        fieldParameter.setTypeForced("java.lang.Integer");
                        break;
                    case "J":
                        fieldParameter.setTypeForced("java.lang.Long");
                        break;
                    case "F":
                        fieldParameter.setTypeForced("java.lang.Float");
                        break;
                    case "D":
                        fieldParameter.setTypeForced("java.lang.Double");
                        break;
                    case "B":
                        fieldParameter.setTypeForced("java.lang.Byte");
                        break;
                    case "Z":
                        fieldParameter.setTypeForced("java.lang.Boolean");
                        break;
                    case "C":
                        fieldParameter.setTypeForced("java.lang.Character");
                        break;
                    case "V":
                        fieldParameter.setTypeForced("java.lang.Void");
                        break;
                }
                TestCandidateMetadata testCandidateMetadata = new TestCandidateMetadata();
                testCandidateMetadata.setTestSubject(fieldParameter);
                MethodCallExpression mainMethod = new MethodCallExpression(
                        "<init>", fieldParameter, new ArrayList<>(), fieldParameter, 0
                );

                mainMethod.setMethodAccess(Opcodes.ACC_PUBLIC);
                testCandidateMetadata.setMainMethod(mainMethod);
//                mockCreatorCandidates.add(testCandidateMetadata);
            } else {
                TestCandidateMetadata metadata = MockFactory.createParameterMock(fieldParameter,
                        objectRoutineContainer.getGenerationConfiguration());
                if (metadata == null) {
                    logger.warn(
                            "unable to create a initializer for field: " + fieldParameter.getType() + " - " + fieldParameter.getName());
                    continue;
                }
                mockCreatorCandidates.add(metadata);

            }
        }


        return mockCreatorCandidates;

    }

//    public List<TestCandidateMetadata> getTestCandidatesForMethod(String className, String methodName, boolean loadCalls) {
//        return sessionInstance.getTestCandidatesForPublicMethod(className, methodName, loadCalls);
//    }

}
