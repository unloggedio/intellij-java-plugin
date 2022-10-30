package com.insidious.plugin.factory.testcase.writer;

import com.insidious.plugin.pojo.Parameter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TestCaseWriter {

//    public static void createMethodCallComment(
//            ObjectRoutineScript objectRoutine,
//            MethodCallExpression methodCallExpression
//    ) {
//        VariableContainer variableContainer = objectRoutine.getCreatedVariables();
//        Parameter returnValue = methodCallExpression.getReturnValue();
//        Parameter exception = methodCallExpression.getException();
//        String callArgumentsString = createMethodParametersString(methodCallExpression.getArguments());
//
//
//        if (returnValue != null) {
//
//            String variableName =
//                    ClassTypeUtils.createVariableNameFromMethodName(
//                            methodCallExpression.getMethodName(),
//                            methodCallExpression.getReturnValue().getType());
//
//            Object value = returnValue.getValue();
//            boolean overrideName = true;
//            if (value instanceof String) {
//                String valueString = (String) value;
//                if (valueString.equals("1") || valueString.equals("0")) {
//                    overrideName = false;
//                }
//            }
//
//
//            Parameter existingVariableById = variableContainer.getParametersById(value);
//            if (existingVariableById != null) {
//                if (overrideName && !returnValue.getName().equals(existingVariableById.getName())) {
//                    returnValue.setName(existingVariableById.getName());
//                }
//            } else {
//                if (returnValue.getName() == null) {
//                    returnValue.setName(variableName);
//                }
////                variableContainer.add(returnValue);
//            }
//
//
////            objectRoutine.addComment(
////                    returnValue.getType() + " " + returnValue.getName() + " = " +
////                            methodCallExpression.getSubject().getName() + "." + methodCallExpression.getMethodName() +
////                            "(" + callArgumentsString + "); // ==> "
////                            + returnValue.getProb().getSerializedValue().length);
//        } else if (exception != null) {
////            objectRoutine.addComment(
////                    methodCallExpression.getSubject().getName() + "." +
////                            methodCallExpression.getMethodName() +
////                            "(" + callArgumentsString + ");" +
////                            " // ==>  throws exception " + exception.getType());
//        }
//    }


    @NotNull
    public static String createMethodParametersString(List<Parameter> variableContainer) {
        if (variableContainer == null) {
            return "";
        }
        StringBuilder parameterStringBuilder = new StringBuilder();

        for (int i = 0; i < variableContainer.size(); i++) {
            Parameter parameter = variableContainer.get(i);

            if (i > 0) {
                parameterStringBuilder.append(", ");
            }

            if (parameter.getType() != null && parameter.getType().endsWith("[]")) {
                parameterStringBuilder.append("any()");
            } else if (parameter.getType() != null && (parameter.getType().equals("Z") || parameter.getType().equals("java.lang.Boolean"))){
                if (parameter.getValue() == 1) {
                    parameterStringBuilder.append("true");
                } else {
                    parameterStringBuilder.append("false");
                }
            } else if (parameter.getName() != null) {
                parameterStringBuilder.append(parameter.getName());
            } else {
                Object parameterValue;
                parameterValue = parameter.getValue();
                String stringValue = parameter.getStringValue();
                if (stringValue == null) {
                    parameterStringBuilder.append(parameterValue);
                } else {
                    parameterStringBuilder.append(stringValue);
                }
            }


        }


        @NotNull String parameterString = parameterStringBuilder.toString();
        return parameterString;
    }


    @NotNull
    public static String
    createMethodParametersStringMock(List<Parameter> variableContainer) {
        if (variableContainer == null) {
            return "";
        }
        StringBuilder parameterStringBuilder = new StringBuilder();

        for (int i = 0; i < variableContainer.size(); i++) {
            Parameter parameter = variableContainer.get(i);

            if (i > 0) {
                parameterStringBuilder.append(", ");
            }

            Object compareAgainst = "";
            String parameterType = parameter.getType();
            if (parameterType != null && parameterType.endsWith("[]")) {
                compareAgainst = "";
            } else if (parameter.getName() != null) {
                compareAgainst = parameter.getName();
            } else {
                compareAgainst = parameter.getValue();
            }

            if (compareAgainst != null && parameterType != null && parameterType.equals("java.lang.String")) {
                parameterStringBuilder.append("matches(" + compareAgainst + ")");
            } else if (parameterType != null && compareAgainst != null
                    && (parameterType.length() == 1 || parameterType.startsWith("java.lang.")
                    && !parameterType.contains(".Object"))
            ) {

                if ((parameterType.equals("Z") || parameterType.equals("java.lang.Boolean")) && parameter.getName() == null) {
                    if (compareAgainst.equals("0")) {
                        compareAgainst = "false";
                    } else {
                        compareAgainst = "true";
                    }
                }

                parameterStringBuilder.append("eq(" + compareAgainst + ")");
            } else {
                parameterStringBuilder.append("any()");
            }


        }


        @NotNull String parameterString = parameterStringBuilder.toString();
        return parameterString;
    }


//    public static void createMethodCallMock(
//            ObjectRoutineScript objectRoutine,
//            MethodCallExpression callExpressionToMock
//    ) {
//
//        Parameter returnValue = callExpressionToMock.getReturnValue();
//
//
//        for (Parameter argument : callExpressionToMock.getArguments().all()) {
//            if (argument.getName() != null) {
//                in(objectRoutine).assignVariable(argument).fromRecordedValue().endStatement();
//            }
//        }
//
//        if (returnValue == null) {
//            Parameter exceptionValue = callExpressionToMock.getException();
//            in(objectRoutine)
//                    .writeExpression(MethodCallExpressionFactory.MockitoWhen(callExpressionToMock))
//                    .writeExpression(MethodCallExpressionFactory.MockitoThenThrow(exceptionValue))
//                    .endStatement();
//
//        } else {
//            if (returnValue.getCreaterExpression() == null) {
//                in(objectRoutine).assignVariable(returnValue).fromRecordedValue().endStatement();
//            } else {
//                MethodCallExpression createrExpression = returnValue.getCreaterExpression();
//
//                VariableContainer arguments = createrExpression.getArguments();
//                for (Parameter parameter : arguments.all()) {
//                    in(objectRoutine)
//                            .assignVariable(parameter)
//                            .fromRecordedValue()
//                            .endStatement();
//                }
//
//
//                in(objectRoutine)
//                        .assignVariable(createrExpression.getReturnValue())
//                        .writeExpression(createrExpression)
//                        .endStatement();
//            }
//
//            in(objectRoutine)
//                    .writeExpression(MethodCallExpressionFactory.MockitoWhen(callExpressionToMock))
//                    .writeExpression(MethodCallExpressionFactory.MockitoThen(returnValue))
//                    .endStatement();
//        }
//
//
//    }

    private static PendingStatement in(ObjectRoutineScript objectRoutine) {
        return new PendingStatement(objectRoutine);
    }


}
