package com.insidious.plugin.factory;

import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class PendingStatement {
    private final ObjectRoutine objectRoutine;
    private final List<Expression> expressionList = new LinkedList<>();
    private Parameter lhsExpression;

    public PendingStatement(ObjectRoutine objectRoutine, Parameter testSubject) {

        this.objectRoutine = objectRoutine;
        this.lhsExpression = testSubject;
    }

    public PendingStatement(ObjectRoutine objectRoutine) {
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

        assert this.expressionList != null;


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
                String parameterString = TestCaseWriter.createMethodParametersString(methodCallExpression.getArguments());
                if (methodCallExpression.getMethodName().equals("<init>")) {
                    assert i == 0;
                    statementBuilder.append("new $T(").append(parameterString).append(")");
                    statementParameters.add(ClassName.bestGuess(methodCallExpression.getReturnValue().getType()));
                } else if (methodCallExpression.getSubject() != null) {
                    statementBuilder.append("$L.$L(").append(parameterString).append(")");
                    statementParameters.add(methodCallExpression.getSubject().getName());
                    statementParameters.add(methodCallExpression.getMethodName());
                } else if (methodCallExpression.getSubject() == null) {
                    if (i > 0) {
                        statementBuilder.append(".");
                    }
                    statementBuilder.append("$L(").append(parameterString).append(")");
                    statementParameters.add(methodCallExpression.getMethodName());
                }

            } else {
                statementBuilder.append(expression.toString());
            }
            i++;
        }


        objectRoutine.addStatement(statementBuilder.toString(), statementParameters);
    }

    public PendingStatement fromRecordedValue() {
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
                    this.expressionList.add(new PlainValueExpression((String) returnValue));

                } else if (returnValue instanceof Long) {
                    if ((long) returnValue == 1) {
                        this.expressionList.add(new PlainValueExpression("true"));
                    } else {
                        this.expressionList.add(new PlainValueExpression("false"));
                    }

                }

            } else if (parameter.getProb().getSerializedValue().length > 0) {
                this.expressionList.add(new PlainValueExpression(new String(parameter.getProb().getSerializedValue())));
            } else {
                this.expressionList.add(new PlainValueExpression(String.valueOf(parameter.getValue())));
            }

        } else {

            Parameter gson = new Parameter();
            gson.setName("gson");
            Parameter parameterValue = new Parameter();
            parameterValue.setValue(new String(lhsExpression.getProb().getSerializedValue()));
            Parameter parameterType = new Parameter();


            parameterType.setValue(targetClassname);
            this.expressionList.add(new MethodCallExpression(
                    "fromJson", gson, VariableContainer.from(List.of(parameterValue, parameterType)),
                    this.lhsExpression, null
            ));

        }


        return this;
    }
}
