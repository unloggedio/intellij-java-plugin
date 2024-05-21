package com.insidious.plugin.factory.testcase.routine;


import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.factory.testcase.TestGenerationState;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.util.ClassTypeUtils;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScript;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScriptContainer;
import com.insidious.plugin.factory.testcase.writer.PendingStatement;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.squareup.javapoet.ClassName;


import javax.lang.model.element.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A convenient container for list of ObjectRoutine. we always have one constructor routine for
 * the object, which is always named &lt;init&gt; and the other methods have proper names. Add
 * statements is redirected to most recently created object routine.
 */
public class ObjectRoutineContainer {
    private final List<ObjectRoutine> objectRoutines = new ArrayList<>();
    private final Parameter testSubject;
    private final TestCaseGenerationConfiguration generationConfiguration;
    private final String testMethodName;
    private String packageName;
    private ObjectRoutine currentRoutine;
    private final VariableContainer fieldsContainer = new VariableContainer();
    private final ObjectRoutine constructor = newRoutine("<init>");
    /**
     * Name for variable for this particular object
     */
    private String name;

    public ObjectRoutineContainer(TestCaseGenerationConfiguration generationConfiguration) {
        this.generationConfiguration = generationConfiguration;
        List<TestCandidateMetadata> testCandidateMetadataList = generationConfiguration.getTestCandidateMetadataList();
        Parameter parameter = testCandidateMetadataList.get(testCandidateMetadataList.size() - 1).getTestSubject();
        ClassName targetClassName = ClassName.bestGuess(parameter.getType());

        this.testSubject = parameter;
        assert parameter.getType() != null;
        this.packageName = targetClassName.packageName();
        this.name = targetClassName.simpleName();
        this.testMethodName = generationConfiguration.getTestMethodName();
        newRoutine("test" + targetClassName.simpleName());

        boolean hasTargetInstanceClassConstructor = false;
        for (TestCandidateMetadata testCandidateMetadata : testCandidateMetadataList) {

            MethodCallExpression methodInfo = testCandidateMetadata.getMainMethod();
            if (methodInfo.getMethodName().equals("<init>")
                    && methodInfo.getReturnValue().getType().equals(testCandidateMetadata.getTestSubject().getType())
            ) {
                hasTargetInstanceClassConstructor = true;
            }
            addMetadata(testCandidateMetadata);
        }
        if (!hasTargetInstanceClassConstructor) {
            TestCandidateMetadata newTestCaseMetadata = new TestCandidateMetadata();
            MethodCallExpression constructorMethod = new MethodCallExpression("<init>", testSubject,
                    Collections.emptyList(),
                    testSubject, 0);
            constructorMethod.setMethodAccess(1);
            newTestCaseMetadata.setMainMethod(constructorMethod);
            newTestCaseMetadata.setTestSubject(testSubject);
            newTestCaseMetadata.setFields(VariableContainer.from(Collections.emptyList()));
            constructor.setTestCandidateList(newTestCaseMetadata);
        }

    }

    public void addFieldParameter(Parameter parameter) {
        fieldsContainer.add(parameter);
    }

    public Parameter getTestSubject() {
        return testSubject;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ObjectRoutine newRoutine(String routineName) {
        for (ObjectRoutine objectRoutine : this.objectRoutines) {
            if (objectRoutine.getRoutineName()
                    .equals(routineName)) {
                this.currentRoutine = objectRoutine;
                return objectRoutine;
            }
        }

        ObjectRoutine newRoutine = new ObjectRoutine(routineName);
        this.objectRoutines.add(newRoutine);
        this.currentRoutine = newRoutine;
        return newRoutine;
    }

    public List<ObjectRoutine> getObjectRoutines() {
        return objectRoutines;
    }

    public ObjectRoutine getConstructor() {
        return constructor;
    }

    public void addMetadata(TestCandidateMetadata newTestCaseMetadata) {
        if (newTestCaseMetadata.getMainMethod().getMethodName().equals("<init>")) {
            constructor.addMetadata(newTestCaseMetadata);
        } else {
            currentRoutine.addMetadata(newTestCaseMetadata);
        }
    }

    public List<Parameter> getVariablesOfType(final String className) {


        List<Parameter> dependentImports = new ArrayList<>();
        for (ObjectRoutine objectRoutine : this.objectRoutines) {

//            dependentImports = objectRoutine.getDependentList()
//                    .stream()
//                    .filter(e -> e != orc)
//                    .map(e -> e.getVariablesOfType(className))
//                    .flatMap(Collection::stream)
//                    .collect(Collectors.toList());

            for (TestCandidateMetadata metadata : objectRoutine.getTestCandidateList()) {
                MethodCallExpression mainMethod = metadata.getMainMethod();
                List<Parameter> mainMethodVariables = extractVariableOfType(className, mainMethod);
                dependentImports.addAll(mainMethodVariables);

                for (MethodCallExpression methodCallExpression : metadata.getCallsList()) {
                    List<Parameter> variables = extractVariableOfType(className, methodCallExpression);
                    dependentImports.addAll(variables);
                }
            }
        }

        return dependentImports;


    }

    private List<Parameter> extractVariableOfType(String className, MethodCallExpression mainMethod) {
        List<Parameter> dependentImports = new ArrayList<>();
        if (mainMethod.getSubject() != null && mainMethod.getSubject()
                .getType() != null && mainMethod.getSubject()
                .getType()
                .startsWith(className)) {
            dependentImports.add(mainMethod.getSubject());
        }
        if (mainMethod.getReturnValue() != null && mainMethod.getReturnValue()
                .getType() != null && mainMethod.getReturnValue()
                .getType()
                .startsWith(className)) {
            dependentImports.add(mainMethod.getSubject());
        }
        if (mainMethod.getArguments() != null && mainMethod.getArguments() != null) {
            for (Parameter parameter : mainMethod.getArguments()) {
                if (parameter.getType() != null && parameter.getType()
                        .startsWith(className)) {
                    dependentImports.add(parameter);
                }
            }
        }


        return dependentImports;
    }

    public ObjectRoutineScriptContainer toObjectRoutineScriptContainer(
            SessionInstance sessionInstance, TestGenerationState testGenerationState
    ) {
        ObjectRoutineScriptContainer container = new ObjectRoutineScriptContainer(this.packageName,
                testGenerationState, generationConfiguration);
        container.setName(getName());


        VariableContainer variableContainer = new VariableContainer();

        for (Parameter parameter : fieldsContainer.all()) {
            variableContainer.add(parameter);
        }
        testGenerationState.setVariableContainer(variableContainer);


        ObjectRoutine constructorRoutine = getConstructor();
        ObjectRoutineScript builderMethodScript = constructorRoutine
                .toObjectRoutineScript(generationConfiguration, testGenerationState, sessionInstance,
                        fieldsContainer.clone());

        builderMethodScript.setRoutineName("setup");
        builderMethodScript.addAnnotation(generationConfiguration.getTestBeforeAnnotationType());
        builderMethodScript.addException(Exception.class);
        builderMethodScript.addModifiers(Modifier.PUBLIC);


        if (!generationConfiguration.useMockitoAnnotations()) {
            container.getObjectRoutines().add(builderMethodScript);
        }


         List<Parameter> constructorNonPojoParams =
                ObjectRoutine.getNonPojoParameters(constructorRoutine.getTestCandidateList(), sessionInstance);


        for (Parameter parameter : fieldsContainer.all()) {
            container.addField(parameter);
        }

        if (!generationConfiguration.useMockitoAnnotations()) {
            for (Parameter parameter : constructorNonPojoParams) {
                container.addField(parameter);
            }
        }


        Parameter mainSubject = getTestSubject();
        if (mainSubject.getName() == null) {
            mainSubject.setName(ClassTypeUtils.createVariableName(mainSubject.getType()));
        }

        VariableContainer classVariableContainer = builderMethodScript.getCreatedVariables();
        classVariableContainer.add(mainSubject);
        testGenerationState.setVariableContainer(classVariableContainer);


        Parameter testUtilClassSubject = new Parameter();
        testUtilClassSubject.setType("io.unlogged.UnloggedTestUtils");
        testUtilClassSubject.setName("UnloggedTestUtils");

        if (!generationConfiguration.useMockitoAnnotations()) {
            for (Parameter parameter : fieldsContainer.all()) {

                if (constructorNonPojoParams.stream().anyMatch(e -> e.getValue() == parameter.getValue())) {
                    continue;
                }

                if (parameter.isPrimitiveType()) {
                    continue;
                }

                classVariableContainer.add(parameter);
                MethodCallExpression injectMethodCall = new MethodCallExpression(
                        "injectField", testUtilClassSubject, Arrays.asList(mainSubject, parameter),
                        null, 0);
                injectMethodCall.setStaticCall(true);
                PendingStatement.in(builderMethodScript, testGenerationState)
                        .writeExpression(injectMethodCall)
                        .endStatement();

            }
        }

        Map<String, Parameter> staticMocks = new HashMap<>();
        for (ObjectRoutine objectRoutine : this.objectRoutines) {
            if (objectRoutine.getRoutineName().equals("<init>")) {
                continue;
            }

            ObjectRoutineScript objectScript =
                    objectRoutine.toObjectRoutineScript(generationConfiguration, testGenerationState, sessionInstance,
                            fieldsContainer.clone());
            container.getObjectRoutines().add(objectScript);

            List<Parameter> staticMockList = objectScript.getStaticMocks();
            for (Parameter staticMock : staticMockList) {
                if (!staticMocks.containsKey(staticMock.getType())) {
                    staticMocks.put(staticMock.getType(), staticMock);
                    classVariableContainer.add(staticMock);
                }
            }


            if (testMethodName != null) {
                container.setTestMethodName(testMethodName);
            } else {
                String testMethodNameFromMethod = objectRoutine.getTestCandidateList()
                        .get(objectRoutine.getTestCandidateList().size() - 1)
                        .getMainMethod().getMethodName();
                container.setTestMethodName(testMethodNameFromMethod);
            }
        }

        for (Parameter staticMock : staticMocks.values()) {
            staticMock.setContainer(true);

            Parameter childParameter = new Parameter();
            childParameter.setType(staticMock.getType());
            childParameter.setName("E");

            staticMock.setTypeForced("org.mockito.MockedStatic");
            staticMock.getTemplateMap()
                    .add(childParameter);

            container.addField(staticMock);

            PendingStatement.in(builderMethodScript, testGenerationState)
                    .assignVariable(staticMock)
                    .writeExpression(MethodCallExpressionFactory.MockStaticClass(
                            ClassName.bestGuess(staticMock.getTemplateMap().get(0).getType()),
                            generationConfiguration))
                    .endStatement();
        }

        if (staticMocks.size() > 0 && !generationConfiguration.useMockitoAnnotations()) {
            // For setting the method with @After / @AfterEach Annotation
            VariableContainer finishedVariableContainer = new VariableContainer();
            ObjectRoutineScript afterEachMethodScript = new ObjectRoutineScript(finishedVariableContainer,
                    generationConfiguration, testGenerationState);

            afterEachMethodScript.setRoutineName("finished");
            afterEachMethodScript.addAnnotation(generationConfiguration.getTestAfterAnnotationType());
            afterEachMethodScript.addException(Exception.class);
            afterEachMethodScript.addModifiers(Modifier.PUBLIC);

            // Writing inside @AfterEach "finished" function
            // Close Static Mock functions
            for (Parameter staticMock : staticMocks.values()) {
                PendingStatement.in(afterEachMethodScript, testGenerationState)
                        .writeExpression(MethodCallExpressionFactory.CloseStaticMock(staticMock))
                        .endStatement();
            }

            // Only add to test file only if the @AfterEach is not empty statements
            if (afterEachMethodScript.getStatements().size() > 0) {
                container.getObjectRoutines().add(afterEachMethodScript);
            }
        }

        return container;
    }

    
    public Set<? extends Parameter> collectFieldsFromRoutines() {

        //            boolean isPresent = false;
        //
        //            Optional<MethodCallExpression> foundUsage = getObjectRoutines()
        //                    .stream()
        //                    .map(ObjectRoutine::getTestCandidateList)
        //                    .flatMap(Collection::stream)
        //                    .map(TestCandidateMetadata::getCallsList)
        //                    .flatMap(Collection::stream)
        //                    .filter(e -> e.getSubject().getValue() == fieldParameter.getValue())
        //                    .findAny();
        //            if (!foundUsage.isPresent()) {
        //                // field is not actually used anywhere, so we don't want to create it
        //                continue;
        //            }
        //            if (!isPresent) {
        //            }

        return getObjectRoutines().stream()
                .map(ObjectRoutine::getTestCandidateList)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .filter(e -> e.getTestSubject().getValue() == testSubject.getValue())
                .map(e -> e.getFields().all())
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public TestCaseGenerationConfiguration getGenerationConfiguration() {
        return generationConfiguration;
    }
}
