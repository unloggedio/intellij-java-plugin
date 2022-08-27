package com.insidious.plugin.factory;

import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.intellij.openapi.util.text.StringUtil;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.stream.Collectors;

public class TestCaseWriter {

    private static final ClassName mockitoClass = ClassName.bestGuess("org.mockito.Mockito");
    private static final ClassName assertClass = ClassName.bestGuess("org.junit.Assert");

    public static void createMethodCallComment(
            ObjectRoutine objectRoutine,
            MethodCallExpression methodCallExpression
    ) {
        VariableContainer variableContainer = objectRoutine.getVariableContainer();
        Parameter returnValue = methodCallExpression.getReturnValue();
        Parameter exception = methodCallExpression.getException();
        String callArgumentsString = createMethodParametersString(methodCallExpression.getArguments());

        String callParametersCommaSeparated = StringUtil.join(methodCallExpression.getArguments()
                        .all()
                        .stream().map(Parameter::getName)
                        .collect(Collectors.toList()),
                ", ");
        if (returnValue != null) {

            String variableName = ClassTypeUtils.createVariableName(returnValue.getType());

            Optional<Parameter> existingVariableById = variableContainer.getParametersById((String) returnValue.getValue());
            if (existingVariableById.isPresent()) {
                returnValue.setName(existingVariableById.get().getName());
            } else {
                if (returnValue.getName() == null) {
                    returnValue.setName(variableName);
                }
                variableContainer.add(returnValue);
            }


            objectRoutine.addComment(
                    returnValue.getType() + " " + returnValue.getName() + " = " +
                            methodCallExpression.getSubject().getName() + "." + methodCallExpression.getMethodName() +
                            "(" + callParametersCommaSeparated + "); // ==> "
                            + returnValue.getProb().getSerializedValue().length);
        } else if (exception != null) {
            objectRoutine.addComment(
                    methodCallExpression.getSubject().getName() + "." +
                            methodCallExpression.getMethodName() +
                            "(" + callParametersCommaSeparated + ");" +
                            " // ==>  throws exception " + exception.getType());
        }
    }


    @NotNull
    public static String createMethodParametersString(VariableContainer variableContainer) {
        StringBuilder parameterStringBuilder = new StringBuilder();

        for (int i = 0; i < variableContainer.count(); i++) {
            Parameter parameter = variableContainer.get(i);

            if (i > 0) {
                parameterStringBuilder.append(", ");
            }

            if (parameter.getType() != null && parameter.getType().endsWith("[]")) {
                parameterStringBuilder.append("any()");
            } else if (parameter.getName() != null) {
                parameterStringBuilder.append(parameter.getName());
            } else {
                Object parameterValue;
                parameterValue = parameter.getValue();
                parameterStringBuilder.append(parameterValue);
            }


        }


        @NotNull String parameterString = parameterStringBuilder.toString();
        return parameterString;
    }


    public static void createMethodCallMock(
            ObjectRoutine objectRoutine,
            MethodCallExpression callExpressionToMock
    ) {
        VariableContainer createdVariableContainer = objectRoutine.getCreatedVariables();
        VariableContainer variableContainer = objectRoutine.getVariableContainer();

        Parameter returnValue = callExpressionToMock.getReturnValue();
        Parameter exceptionValue = callExpressionToMock.getException();

        String callArgumentsString =
                TestCaseWriter.createMethodParametersString(callExpressionToMock.getArguments());

        String methodNameToMock = callExpressionToMock.getMethodName();
        if (returnValue != null) {

            for (Parameter argument : callExpressionToMock.getArguments().all()) {
                if (argument.getName() != null) {
                    in(objectRoutine).assignVariable(argument).fromRecordedValue().endStatement();
                }
            }


            String variableName = ClassTypeUtils.createVariableName(returnValue.getType());

            Optional<Parameter> existingVariableById =
                    variableContainer.getParametersById((String) returnValue.getValue());

            if (existingVariableById.isPresent()) {
                returnValue.setName(existingVariableById.get().getName());
            } else {
                if (returnValue.getName() == null) {
                    returnValue.setName(variableName);
                }
            }

            String returnTypeValue = returnValue.getType();

            boolean isArray = false;
            if (returnTypeValue.startsWith("[")) {
                returnTypeValue = returnTypeValue.substring(1);
                isArray = true;
            }
            ClassName returnTypeClass = ClassName.bestGuess(returnTypeValue);

            if (returnValue.getType().length() < 2) {

                TypeName returnType = ClassTypeUtils.getActualClassNameForBasicType(returnValue.getType());

                Object callReturnValue =
                        returnValue.getValue();
                if (callReturnValue.equals("0")) {
                    callReturnValue = "false";
                } else {
                    callReturnValue = "true";
                }
                if (returnValue.getName() == null) {

                    objectRoutine.addStatement(
                            "$T.when($L.$L($L)).thenReturn($L)",
                            mockitoClass, callExpressionToMock.getSubject().getName(),
                            methodNameToMock, callArgumentsString,
                            callReturnValue
                    );
                } else {
                    in(objectRoutine).assignVariable(returnValue).fromRecordedValue().endStatement();

                    in(objectRoutine)
                            .writeExpression(MethodCallExpressionFactory.MockitoWhen(callExpressionToMock))
                            .writeExpression(MethodCallExpressionFactory.MockitoThen(returnValue))
                            .endStatement();

                }


            } else if (returnValue.getType().equals("java.lang.String")) {
                if (returnValue.getName() == null) {
                    objectRoutine.addStatement(
                            "$T.when($L.$L($L)).thenReturn($L)",
                            mockitoClass, callExpressionToMock.getSubject().getName(),
                            methodNameToMock, callArgumentsString,
                            new String(returnValue.getProb().getSerializedValue())
                    );
                } else {
                    if (createdVariableContainer.getParameterByName(returnValue.getName()) == null) {

                        objectRoutine.addStatement(
                                "$T $L = $L",
                                returnTypeClass, returnValue.getName(),
                                new String(returnValue.getProb().getSerializedValue())

                        );
                        createdVariableContainer.add(returnValue);
                    }
                    objectRoutine.addStatement(
                            "$T.when($L.$L($L)).thenReturn($L)",
                            mockitoClass, callExpressionToMock.getSubject().getName(),
                            methodNameToMock, callArgumentsString,
                            returnValue.getName()
                    );
                    variableContainer.add(returnValue);

                }

            } else {
                if (returnValue.getName() == null) {
                    objectRoutine.addStatement(
                            "$T.when($L.$L($L)).thenReturn(gson.fromJson($S, $T.class))",
                            mockitoClass, callExpressionToMock.getSubject().getName(),
                            methodNameToMock, callArgumentsString,
                            new String(returnValue.getProb().getSerializedValue()),
                            returnTypeClass
                    );
                } else {
                    if (createdVariableContainer.getParameterByName(returnValue.getName()) == null) {

                        objectRoutine.addStatement(
                                "$T $L = gson.fromJson($S, $T.class)",
                                returnTypeClass, returnValue.getName(),
                                new String(returnValue.getProb().getSerializedValue()), returnTypeClass

                        );
                        createdVariableContainer.add(returnValue);
                    }
                    objectRoutine.addStatement(
                            "$T.when($L.$L($L)).thenReturn($L)",
                            mockitoClass, callExpressionToMock.getSubject().getName(),
                            methodNameToMock, callArgumentsString,
                            returnValue.getName()
                    );
                    variableContainer.add(returnValue);

                }

            }


        } else if (exceptionValue != null) {
            objectRoutine.addStatement(
                    "$T.when($L.$L($L)).thenThrow($T.class)",
                    mockitoClass, callExpressionToMock.getSubject().getName(),
                    methodNameToMock, callArgumentsString,
                    ClassName.bestGuess(exceptionValue.getType())
            );
        }
    }

    private static PendingStatement in(ObjectRoutine objectRoutine) {
        return new PendingStatement(objectRoutine);
    }


}
