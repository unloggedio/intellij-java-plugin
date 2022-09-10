package com.insidious.plugin.factory.testcase.writer;

import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.factory.testcase.expression.*;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class PendingStatement {
    public static final ClassName TYPE_TOKEN_CLASS = ClassName.bestGuess("com.google.gson.reflect.TypeToken");
    public static final ClassName LIST_CLASS = ClassName.bestGuess("java.util.List");
    private final ObjectRoutineScript objectRoutine;
    private final List<Expression> expressionList = new LinkedList<>();
    private Parameter lhsExpression;

    public PendingStatement(ObjectRoutineScript objectRoutine, Parameter testSubject) {

        this.objectRoutine = objectRoutine;
        this.lhsExpression = testSubject;
    }

    public PendingStatement(ObjectRoutineScript objectRoutine) {
        this.objectRoutine = objectRoutine;
    }

    public PendingStatement writeExpression(Expression expression) {
        this.expressionList.add(expression);
        return this;
    }

    public PendingStatement assignVariable(Parameter lhsExpression) {
        this.lhsExpression = lhsExpression;
        return this;
    }

    public void endStatement() {

        StringBuilder statementBuilder = new StringBuilder();
        List<Object> statementParameters = new LinkedList<>();

        if (lhsExpression != null && !lhsExpression.getType().equals("V")) {

            @Nullable TypeName lhsTypeName = ClassTypeUtils.createTypeFromName(lhsExpression.getType());
            if (!objectRoutine.getCreatedVariables().contains(lhsExpression.getName())) {
                objectRoutine.getCreatedVariables().add(lhsExpression);
                statementBuilder.append("$T $L");
                statementParameters.add(lhsTypeName);
                statementParameters.add(lhsExpression.getName());
            } else {
                statementBuilder.append("$L");
                statementParameters.add(lhsExpression.getName());
            }
            statementBuilder.append(" = ");

        }

        int i = 0;
        for (Expression expression : this.expressionList) {
            if (expression instanceof MethodCallExpression) {
                MethodCallExpression methodCallExpression = (MethodCallExpression) expression;

//                for (Parameter parameter : methodCallExpression.getArguments().all()) {
//                    objectRoutine.getCreatedVariables().add(parameter);
//
//                }


                String parameterString = TestCaseWriter.createMethodParametersString(methodCallExpression.getArguments());
                if (methodCallExpression.getMethodName().equals("<init>")) {
                    assert i == 0;
                    statementBuilder.append("new $T(").append(parameterString).append(")");
                    statementParameters.add(ClassName.bestGuess(methodCallExpression.getReturnValue().getType()));
                } else if (methodCallExpression.getMethodName().equals("fromJson")
                        && methodCallExpression.getSubject().equals(MethodCallExpressionFactory.GsonClass)) {


                    List<? extends Parameter> variables = methodCallExpression.getArguments().all();

                    Parameter objectToDeserialize = variables.get(0);

                    if (objectToDeserialize.isContainer()) {

                        statementBuilder.append("$L.$L($S, new $T<$T<$T>>(){}.getType())");
                        statementParameters.add(methodCallExpression.getSubject().getName());
                        statementParameters.add(methodCallExpression.getMethodName());

                        statementParameters.add(new String(objectToDeserialize.getProb().getSerializedValue()));

                        statementParameters.add(TYPE_TOKEN_CLASS);
                        statementParameters.add(LIST_CLASS);

                        statementParameters.add(ClassName.bestGuess(objectToDeserialize.getTemplateMap().get("E").getType()));


                    } else {
                        statementBuilder.append("$L.$L($S, $T.class)");
                        statementParameters.add(methodCallExpression.getSubject().getName());
                        statementParameters.add(methodCallExpression.getMethodName());

                        statementParameters.add(new String(objectToDeserialize.getProb().getSerializedValue()));
                        statementParameters.add(ClassName.bestGuess(objectToDeserialize.getType()));

                    }

                }  else if (methodCallExpression.getMethodName().equals("injectField")
                        && methodCallExpression.getSubject() == null) {


                    List<? extends Parameter> variables = methodCallExpression.getArguments().all();

                    statementBuilder.append("injectField($L, $S, $L)");
                    statementParameters.add(variables.get(0).getName());
                    statementParameters.add(variables.get(1).getName());
                    statementParameters.add(variables.get(1).getName());

                }  else if (methodCallExpression.getMethodName().equals("thenThrow")
                        && methodCallExpression.getSubject() == null) {


                    List<? extends Parameter> variables = methodCallExpression.getArguments().all();

                    statementBuilder.append(".$L($T.class)");
                    statementParameters.add(methodCallExpression.getMethodName());
                    statementParameters.add(ClassName.bestGuess(variables.get(0).getType()));

                } else {
                    Parameter callExpressionSubject = methodCallExpression.getSubject();
                    if (callExpressionSubject != null) {

                        if (Objects.equals(callExpressionSubject.getName(), "Mockito")
                                || Objects.equals(callExpressionSubject.getName(), "Assert")) {
                            statementBuilder.append("$T.$L(").append(parameterString).append(")");
                            statementParameters.add(ClassName.bestGuess(callExpressionSubject.getType()));
                            statementParameters.add(methodCallExpression.getMethodName());
                        } else {
                            statementBuilder.append("$L.$L(").append(parameterString).append(")");
                            statementParameters.add(callExpressionSubject.getName());
                            statementParameters.add(methodCallExpression.getMethodName());

                        }
                    } else {
                        if (i > 0) {
                            statementBuilder.append(".");
                        }
                        statementBuilder.append("$L(").append(parameterString).append(")");
                        statementParameters.add(methodCallExpression.getMethodName());
                    }
                }

            } else if (expression instanceof StringExpression) {
                statementBuilder.append("$S");
                statementParameters.add(expression.toString());
            } else {
                statementBuilder.append(expression.toString());
            }
            i++;
        }


        String statement = statementBuilder.toString();
        objectRoutine.addStatement(statement, statementParameters);
    }

    public PendingStatement fromRecordedValue() {
        if (lhsExpression == null) {
            return this;
        }
        String targetClassname = lhsExpression.getType();

        if (targetClassname.startsWith("java.lang") || targetClassname.length() == 1) {

            Parameter parameter = lhsExpression;


            Object returnValue = parameter.getValue();
            if (targetClassname.equals("Z")) {
                if (returnValue instanceof String) {
                    if (Objects.equals(returnValue, "0")) {
                        returnValue = "false";
                    } else {
                        returnValue = "true";
                    }
                    this.expressionList.add(MethodCallExpressionFactory.PlainValueExpression((String) returnValue));

                } else if (returnValue instanceof Long) {
                    if ((long) returnValue == 1) {
                        this.expressionList.add(MethodCallExpressionFactory.PlainValueExpression("true"));
                    } else {
                        this.expressionList.add(MethodCallExpressionFactory.PlainValueExpression("false"));
                    }

                }

            } else if (parameter.getProb().getSerializedValue().length > 0) {
                String stringValue = new String(parameter.getProb().getSerializedValue());
                stringValue = stringValue.replaceAll("\\$", "\\$\\$");

                this.expressionList.add(MethodCallExpressionFactory.PlainValueExpression(stringValue));
            } else {
                String stringValue = String.valueOf(parameter.getValue());
                this.expressionList.add(MethodCallExpressionFactory.PlainValueExpression(stringValue));
            }

        } else {

//            Parameter parameterValue = new Parameter();
//            parameterValue.setValue(new String(lhsExpression.getProb().getSerializedValue()));
//            parameterValue.setType(lhsExpression.getType());
            this.expressionList.add(MethodCallExpressionFactory.FromJson(lhsExpression));

        }


        return this;
    }
}
