package com.insidious.plugin.factory.writer;

import com.insidious.plugin.factory.ClassTypeUtils;
import com.insidious.plugin.factory.ObjectRoutine;
import com.insidious.plugin.factory.VariableContainer;
import com.insidious.plugin.factory.expression.MethodCallExpressionFactory;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.intellij.openapi.util.text.StringUtil;
import com.squareup.javapoet.ClassName;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.stream.Collectors;

public class TestCaseWriter {

    public static void createMethodCallComment(
            ObjectRoutine objectRoutine,
            MethodCallExpression methodCallExpression
    ) {
        VariableContainer variableContainer = objectRoutine.getVariableContainer();
        Parameter returnValue = methodCallExpression.getReturnValue();
        Parameter exception = methodCallExpression.getException();
        String callArgumentsString = createMethodParametersString(methodCallExpression.getArguments());

//        String callParametersCommaSeparated = StringUtil.join(methodCallExpression.getArguments()
//                        .all()
//                        .stream().map(Parameter::getName)
//                        .collect(Collectors.toList()),
//                ", ");
        if (returnValue != null) {

            String variableName = ClassTypeUtils.createVariableName(returnValue.getType());

            Object value = returnValue.getValue();
            boolean overrideName = true;
            if (value instanceof String) {
                String valueString = (String) value;
                if (valueString.equals("1") || valueString.equals("0")) {
                    overrideName = false;
                }
            }


            Optional<Parameter> existingVariableById = variableContainer.getParametersById((String) value);
            if (existingVariableById.isPresent()) {
                if (overrideName) {
                    returnValue.setName(existingVariableById.get().getName());
                }
            } else {
                if (returnValue.getName() == null) {
                    returnValue.setName(variableName);
                }
                variableContainer.add(returnValue);
            }


            objectRoutine.addComment(
                    returnValue.getType() + " " + returnValue.getName() + " = " +
                            methodCallExpression.getSubject().getName() + "." + methodCallExpression.getMethodName() +
                            "(" + callArgumentsString + "); // ==> "
                            + returnValue.getProb().getSerializedValue().length);
        } else if (exception != null) {
            objectRoutine.addComment(
                    methodCallExpression.getSubject().getName() + "." +
                            methodCallExpression.getMethodName() +
                            "(" + callArgumentsString + ");" +
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

        Parameter returnValue = callExpressionToMock.getReturnValue();



        for (Parameter argument : callExpressionToMock.getArguments().all()) {
            if (argument.getName() != null) {
                in(objectRoutine).assignVariable(argument).fromRecordedValue().endStatement();
            }
        }

        if (returnValue == null) {
            Parameter exceptionValue = callExpressionToMock.getException();
            in(objectRoutine)
                    .writeExpression(MethodCallExpressionFactory.MockitoWhen(callExpressionToMock))
                    .writeExpression(MethodCallExpressionFactory.MockitoThenThrow(exceptionValue))
                    .endStatement();

        } else {
            in(objectRoutine).assignVariable(returnValue).fromRecordedValue().endStatement();

            in(objectRoutine)
                    .writeExpression(MethodCallExpressionFactory.MockitoWhen(callExpressionToMock))
                    .writeExpression(MethodCallExpressionFactory.MockitoThen(returnValue))
                    .endStatement();
        }




    }

    private static PendingStatement in(ObjectRoutine objectRoutine) {
        return new PendingStatement(objectRoutine);
    }


}
