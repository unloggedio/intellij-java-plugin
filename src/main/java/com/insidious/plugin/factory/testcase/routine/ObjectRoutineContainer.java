package com.insidious.plugin.factory.testcase.routine;


import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.factory.testcase.expression.Expression;
import com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScript;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScriptContainer;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import lombok.AllArgsConstructor;

import javax.lang.model.element.Modifier;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.insidious.plugin.factory.testcase.writer.TestScriptWriter.in;

/**
 * A convenient container for list of ObjectRoutine. we always have one constructor routine for
 * the object, which is always named &lt;init&gt; and the other methods have proper names. Add
 * statements is redirected to most recently created object routine.
 */
@AllArgsConstructor
public class ObjectRoutineContainer {
    private String packageName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private final List<ObjectRoutine> objectRoutines = new LinkedList<>();


    private ObjectRoutine constructor = newRoutine("<init>");
    private ObjectRoutine currentRoutine;

    /**
     * Name for variable for this particular object
     */
    private String name;

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

    public ObjectRoutineContainer(String packageName) {
        this.packageName =packageName;
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
        currentRoutine.addMetadata(newTestCaseMetadata);
    }

    public List<Parameter> getVariablesOfType(final String className) {


        List<Parameter> dependentImports = new LinkedList<>();
        ObjectRoutineContainer orc = this;
        for (ObjectRoutine objectRoutine : this.objectRoutines) {


            dependentImports = objectRoutine.getDependentList()
                    .stream()
                    .filter(e -> e != orc)
                    .map(e ->  e.getVariablesOfType(className))
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
        for (Parameter parameter : mce.getArguments().all()) {
            if (parameter.getType() != null && parameter.getType().startsWith(className)) {
                dependentImports.add(parameter);
            }
        }


        return dependentImports;
    }

    public ObjectRoutineScriptContainer toRoutineScript() {
        ObjectRoutineScriptContainer container = new ObjectRoutineScriptContainer(this.packageName);
        ObjectRoutineScript builderMethodScript = container.getConstructor();

        ObjectRoutine constructorRoutine = getConstructor();

        MethodSpec.Builder builder = MethodSpec.methodBuilder("setup");

        builder.addModifiers(Modifier.PUBLIC);
        builder.addAnnotation(ClassName.bestGuess("org.junit.Before"));
        builder.addException(Exception.class);


        Set<? extends Parameter> allFields = getObjectRoutines().stream()
                .map(ObjectRoutine::getTestCandidateList)
                .flatMap(Collection::stream)
                .map(e -> e.getFields().all())
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        TestCandidateMetadata firstTestMetadata = constructorRoutine.getTestCandidateList().get(0);
        MethodCallExpression mainSubjectConstructorExpression = (MethodCallExpression) firstTestMetadata.getMainMethod();
        Parameter mainSubject = mainSubjectConstructorExpression.getSubject();
        Parameter returnValue = mainSubjectConstructorExpression.getReturnValue();
        builderMethodScript.getCreatedVariables().add(mainSubject);

//
//        fieldSpecList.add(
//                FieldSpec.builder(
//                        ClassName.bestGuess(mainSubject.getType()),
//                        mainSubject.getName(), Modifier.PRIVATE
//                ).build()
//        );

        in(builderMethodScript).assignVariable(returnValue).writeExpression(mainSubjectConstructorExpression).endStatement();


        for (Parameter parameter : allFields) {
//            fieldSpecList.add(
//                    FieldSpec.builder(
//                            ClassName.bestGuess(parameter.getType()),
//                            parameter.getName(), Modifier.PRIVATE
//                    ).build()
//            );

            builderMethodScript.getCreatedVariables().add(parameter);
            in(builderMethodScript).assignVariable(parameter).writeExpression(
                    MethodCallExpressionFactory.MockClass(ClassName.bestGuess(parameter.getType()))
            ).endStatement();

            in(builderMethodScript).writeExpression(
                    new MethodCallExpression("injectField", null,
                            VariableContainer.from(List.of(
                                    mainSubject, parameter
                            )), null, null)).endStatement();

        }

        return container;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }


//    public ObjectRoutineScriptContainer toRoutineScriptContainer(VariableContainer variableContainer) {
//        ObjectRoutineScriptContainer orsc = new ObjectRoutineScriptContainer();
//
//        orsc.addScriptsFromRoutineContainer(this, variableContainer);
//
//        return orsc;
//    }
}
