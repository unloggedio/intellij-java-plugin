package com.insidious.plugin.factory.testcase.routine;


import com.insidious.plugin.factory.testcase.TestGenerationState;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.factory.testcase.expression.Expression;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScript;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScriptContainer;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.squareup.javapoet.ClassName;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Modifier;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
            if (methodInfo.getReturnValue() == null || methodInfo.getReturnValue().getProb() == null) {
                continue;
            }
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

    public ObjectRoutine getRoutineByName(String name) {
        for (ObjectRoutine objectRoutine : this.objectRoutines) {
            if (objectRoutine.getRoutineName().equals(name)) {
                return objectRoutine;
            }
        }
        return null;

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
        ObjectRoutineScriptContainer container = new ObjectRoutineScriptContainer(this.packageName, testGenerationState);
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
        VariableContainer classVariableContainer = builderMethodScript.getCreatedVariables();
        classVariableContainer.add(mainSubject);


        for (Parameter parameter : allFields) {

            container.addField(parameter);

            classVariableContainer.add(parameter);
            MethodCallExpression.in(builderMethodScript).writeExpression(
                    new MethodCallExpression("injectField", null,
                            List.of(mainSubject, parameter), null, 0)).endStatement();

        }

        for (ObjectRoutine objectRoutine : this.objectRoutines) {
            if (objectRoutine.getRoutineName().equals("<init>")) {
                continue;
            }

            ObjectRoutineScript objectScript =
                    objectRoutine.toObjectScript(classVariableContainer.clone(), generationConfiguration, testGenerationState);
            container.getObjectRoutines().add(objectScript);

            String testMethodName = ((MethodCallExpression) objectRoutine.getTestCandidateList().get(
                    objectRoutine.getTestCandidateList().size() - 1).getMainMethod()).getMethodName();

            container.setTestMethodName(testMethodName);
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

}
