package com.insidious.plugin.factory.testcase.routine;


import com.insidious.plugin.factory.testcase.TestGenerationState;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.factory.testcase.expression.Expression;
import com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScript;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScriptContainer;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.squareup.javapoet.ClassName;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import static com.insidious.plugin.pojo.MethodCallExpression.in;

/**
 * A convenient container for list of ObjectRoutine. we always have one constructor routine for
 * the object, which is always named &lt;init&gt; and the other methods have proper names. Add
 * statements is redirected to most recently created object routine.
 */
@AllArgsConstructor
public class ObjectRoutineContainer {
    private final List<ObjectRoutine> objectRoutines = new LinkedList<>();
    private final Parameter testSubject;
    private final TestCaseGenerationConfiguration generationConfiguration;
    private String packageName;
    private ObjectRoutine currentRoutine;
    private ObjectRoutine constructor = newRoutine("<init>");
    /**
     * Name for variable for this particular object
     */
    private String name;

    public ObjectRoutineContainer(TestCaseGenerationConfiguration generationConfiguration) {
        this.generationConfiguration = generationConfiguration;
        Parameter parameter = generationConfiguration.getTestCandidateMetadataList().get(0).getTestSubject();
        ClassName targetClassName = ClassName.bestGuess(parameter.getType());

        this.testSubject = parameter;
        assert parameter.getType() != null;
        ClassName className = ClassName.bestGuess(parameter.getType());
        this.packageName = className.packageName();
        this.name = className.simpleName();
        newRoutine("test" + targetClassName.simpleName());

        for (TestCandidateMetadata testCandidateMetadata : generationConfiguration.getTestCandidateMetadataList()) {

            MethodCallExpression methodInfo = (MethodCallExpression) testCandidateMetadata.getMainMethod();
//            if (methodInfo.getReturnValue() == null || methodInfo.getReturnValue().getProb() == null) {
//                continue;
//            }
            if (methodInfo.getMethodName().equals("<init>")) {
                constructor.setTestCandidateList(testCandidateMetadata);
            } else {
                addMetadata(testCandidateMetadata);
            }
        }

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
            if (objectRoutine.getRoutineName().equals(routineName)) {
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
        if (((MethodCallExpression) (newTestCaseMetadata.getMainMethod())).getMethodName().equals("<init>")) {
            constructor.addMetadata(newTestCaseMetadata);
        } else {
            currentRoutine.addMetadata(newTestCaseMetadata);
        }
    }

    public List<Parameter> getVariablesOfType(final String className) {


        List<Parameter> dependentImports = new LinkedList<>();
        ObjectRoutineContainer orc = this;
        for (ObjectRoutine objectRoutine : this.objectRoutines) {


            dependentImports = objectRoutine.getDependentList()
                    .stream()
                    .filter(e -> e != orc)
                    .map(e -> e.getVariablesOfType(className))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());

            for (TestCandidateMetadata metadatum : objectRoutine.getTestCandidateList()) {
                Expression mainMethod = metadatum.getMainMethod();
                if (mainMethod instanceof MethodCallExpression) {
                    List<Parameter> variables = extractVariableOfType(className, (MethodCallExpression) mainMethod);
                    dependentImports.addAll(variables);
                }

                for (MethodCallExpression methodCallExpression : metadatum.getCallsList()) {
                    if (mainMethod instanceof MethodCallExpression) {
                        List<Parameter> variables = extractVariableOfType(className, methodCallExpression);
                        dependentImports.addAll(variables);
                    }
                }

            }


        }

        return dependentImports;


    }

    private List<Parameter> extractVariableOfType(String className, MethodCallExpression mainMethod) {
        List<Parameter> dependentImports = new LinkedList<>();
        MethodCallExpression mce = mainMethod;
        if (mce.getSubject() != null && mce.getSubject().getType() != null && mce.getSubject().getType().startsWith(className)) {
            dependentImports.add(mce.getSubject());
        }
        if (mce.getReturnValue() != null && mce.getReturnValue().getType() != null && mce.getReturnValue().getType().startsWith(className)) {
            dependentImports.add(mce.getSubject());
        }
        if (mce.getArguments() != null && mce.getArguments() != null) {
            for (Parameter parameter : mce.getArguments()) {
                if (parameter.getType() != null && parameter.getType().startsWith(className)) {
                    dependentImports.add(parameter);
                }
            }
        }


        return dependentImports;
    }

    public ObjectRoutineScriptContainer toRoutineScript() {
        TestGenerationState testGenerationState = new TestGenerationState();
        ObjectRoutineScriptContainer container = new ObjectRoutineScriptContainer(this.packageName,
                testGenerationState, generationConfiguration);
        container.setName(getName());


        VariableContainer variableContainer = new VariableContainer();

        for (Parameter parameter : this.collectFieldsFromRoutines()) {
            variableContainer.add(parameter);
        }


        ObjectRoutineScript builderMethodScript = getConstructor()
                .toObjectScript(variableContainer, generationConfiguration, testGenerationState);

        container.getObjectRoutines().add(builderMethodScript);

        builderMethodScript.setRoutineName("setup");
        builderMethodScript.addAnnotation(generationConfiguration.getTestBeforeAnnotationType());
        builderMethodScript.addException(Exception.class);
        builderMethodScript.addModifiers(Modifier.PUBLIC);


        Set<? extends Parameter> allFields = collectFieldsFromRoutines();

        Parameter mainSubject = getTestSubject();
        if (mainSubject.getName() == null) {
            mainSubject.setName(ClassTypeUtils.createVariableName(mainSubject.getType()));
        }

        VariableContainer classVariableContainer = builderMethodScript.getCreatedVariables();
        classVariableContainer.add(mainSubject);


        Parameter testUtilClassSubject = new Parameter();
        testUtilClassSubject.setType("io.unlogged.UnloggedTestUtils");
        testUtilClassSubject.setName("UnloggedTestUtils");
        for (Parameter parameter : allFields) {

            container.addField(parameter);

            classVariableContainer.add(parameter);
            MethodCallExpression injectMethodCall = new MethodCallExpression(
                    "injectField", testUtilClassSubject,
                    List.of(mainSubject, parameter), null, 0);
            injectMethodCall.setStaticCall(true);
            MethodCallExpression.in(builderMethodScript).writeExpression(injectMethodCall).endStatement();

        }

        Map<String, Parameter> staticMocks = new HashMap<>();
        for (ObjectRoutine objectRoutine : this.objectRoutines) {
            if (objectRoutine.getRoutineName().equals("<init>")) {
                continue;
            }

            ObjectRoutineScript objectScript =
                    objectRoutine.toObjectScript(classVariableContainer.clone(), generationConfiguration, testGenerationState);
            container.getObjectRoutines().add(objectScript);

            List<Parameter> staticMockList = objectScript.getStaticMocks();
            for (Parameter staticMock : staticMockList) {
                if (!staticMocks.containsKey(staticMock.getType())) {
                    staticMocks.put(staticMock.getType(), staticMock);
                    classVariableContainer.add(staticMock);
                }
            }


            String testMethodName = ((MethodCallExpression) objectRoutine.getTestCandidateList().get(
                    objectRoutine.getTestCandidateList().size() - 1).getMainMethod()).getMethodName();

            container.setTestMethodName(testMethodName);
        }

        for (Parameter staticMock : staticMocks.values()) {
            staticMock.setContainer(true);
            Parameter childParameter = new Parameter();
            childParameter.setType(staticMock.getType());
            staticMock.setTypeForced("org.mockito.MockedStatic");
            staticMock.getTemplateMap().put("E", childParameter);

            container.addField(staticMock);

            in(builderMethodScript)
                    .assignVariable(staticMock)
                    .writeExpression(
                            MethodCallExpressionFactory.MockStaticClass(
                                    ClassName.bestGuess(staticMock.getTemplateMap().get("E").getType()), generationConfiguration))
                    .endStatement();
        }

        // For setting the method with @After / @AfterEach Annotation
        VariableContainer finishedVariableContainer = new VariableContainer();
        ObjectRoutineScript afterEachMethodScript = new ObjectRoutineScript(finishedVariableContainer, generationConfiguration);

        afterEachMethodScript.setRoutineName("finished");
        afterEachMethodScript.addAnnotation(generationConfiguration.getTestAfterAnnotationType());
        afterEachMethodScript.addException(Exception.class);
        afterEachMethodScript.addModifiers(Modifier.PUBLIC);

        // Writing inside @AfterEach "finished" function
        // Close Static Mock functions
        for (Parameter staticMock : staticMocks.values()) {
            in(afterEachMethodScript)
                    .writeExpression(
                            MethodCallExpressionFactory.CloseStaticMock(staticMock))
                    .endStatement();
        }

        // Only add to test file only if the @AfterEach is not empty statements
        if(afterEachMethodScript.getStatements().size()>0) {
            container.getObjectRoutines().add(afterEachMethodScript);
        }

        return container;
    }

    @NotNull
    private Set<? extends Parameter> collectFieldsFromRoutines() {
        return getObjectRoutines().stream()
                .map(ObjectRoutine::getTestCandidateList)
                .flatMap(Collection::stream)
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
