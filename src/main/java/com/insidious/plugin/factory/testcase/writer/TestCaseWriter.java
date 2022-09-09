package com.insidious.plugin.factory.testcase.writer;

import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class TestCaseWriter {

    public static void createMethodCallComment(
            ObjectRoutineScript objectRoutine,
            MethodCallExpression methodCallExpression
    ) {
        VariableContainer variableContainer = objectRoutine.getCreatedVariables();
        Parameter returnValue = methodCallExpression.getReturnValue();
        Parameter exception = methodCallExpression.getException();
        String callArgumentsString = createMethodParametersString(methodCallExpression.getArguments());


        if (returnValue != null) {

            String variableName =
                    ClassTypeUtils.createVariableNameFromMethodName(
                            methodCallExpression.getMethodName(),
                            methodCallExpression.getReturnValue().getType());

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
                if (overrideName && !returnValue.getName().equals(existingVariableById.get().getName())) {
                    returnValue.setName(existingVariableById.get().getName());
                }
            } else {
                if (returnValue.getName() == null) {
                    returnValue.setName(variableName);
                }
//                variableContainer.add(returnValue);
            }


//            objectRoutine.addComment(
//                    returnValue.getType() + " " + returnValue.getName() + " = " +
//                            methodCallExpression.getSubject().getName() + "." + methodCallExpression.getMethodName() +
//                            "(" + callArgumentsString + "); // ==> "
//                            + returnValue.getProb().getSerializedValue().length);
        } else if (exception != null) {
//            objectRoutine.addComment(
//                    methodCallExpression.getSubject().getName() + "." +
//                            methodCallExpression.getMethodName() +
//                            "(" + callArgumentsString + ");" +
//                            " // ==>  throws exception " + exception.getType());
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


    @NotNull
    public static String createMethodParametersStringMock(VariableContainer variableContainer) {
        StringBuilder parameterStringBuilder = new StringBuilder();

        for (int i = 0; i < variableContainer.count(); i++) {
            Parameter parameter = variableContainer.get(i);

            if (i > 0) {
                parameterStringBuilder.append(", ");
            }

            if (parameter.getType() != null && parameter.getType().endsWith("[]")) {
                parameterStringBuilder.append("any()");
            } else if (parameter.getName() != null) {
                if (parameter.getType().equals("java.lang.String")) {
                    parameterStringBuilder.append("matches(" + parameter.getName() + ")");
                } else {
                    parameterStringBuilder.append("any()");
                }
            } else {
                Object parameterValue;
                parameterValue = parameter.getValue();
                if (parameterValue != null && parameter.getType().equals("java.lang.String")) {
                    parameterStringBuilder.append("matches(" + parameterValue + ")");
                } else {
                    parameterStringBuilder.append("any()");
                }
            }


        }


        @NotNull String parameterString = parameterStringBuilder.toString();
        return parameterString;
    }


    public static void createMethodCallMock(
            ObjectRoutineScript objectRoutine,
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
            if (returnValue.getCreaterExpression() == null) {
                in(objectRoutine).assignVariable(returnValue).fromRecordedValue().endStatement();
            } else {
                MethodCallExpression createrExpression = returnValue.getCreaterExpression();

                VariableContainer arguments = createrExpression.getArguments();
                for (Parameter parameter : arguments.all()) {
                    in(objectRoutine)
                            .assignVariable(parameter)
                            .fromRecordedValue()
                            .endStatement();
                }


                in(objectRoutine)
                        .assignVariable(createrExpression.getReturnValue())
                        .writeExpression(createrExpression)
                        .endStatement();
            }

            in(objectRoutine)
                    .writeExpression(MethodCallExpressionFactory.MockitoWhen(callExpressionToMock))
                    .writeExpression(MethodCallExpressionFactory.MockitoThen(returnValue))
                    .endStatement();
        }


    }

    private static PendingStatement in(ObjectRoutineScript objectRoutine) {
        return new PendingStatement(objectRoutine);
    }


}
