package com.insidious.plugin.factory;

import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.squareup.javapoet.ClassName;

import java.util.LinkedList;
import java.util.List;

public class PendingStatement {
    private final ObjectRoutine objectRoutine;
    private Parameter lhsExpression;
    private MethodCallExpression expression;

    public PendingStatement(ObjectRoutine objectRoutine, Parameter testSubject) {

        this.objectRoutine = objectRoutine;
        this.lhsExpression = testSubject;
    }

    public PendingStatement(ObjectRoutine objectRoutine) {
        this.objectRoutine = objectRoutine;
    }

    public PendingStatement writeExpression(MethodCallExpression expression) {
        this.expression = expression;
        return this;
    }

    public PendingStatement assignVariable(Parameter lhsExpression) {
        this.lhsExpression = lhsExpression;
        return this;
    }

    public void endStatement() {

        assert this.expression != null;

        String parameterString =
                TestCaseWriter.createMethodParametersString(this.expression.getArguments());

        StringBuilder statementBuilder = new StringBuilder();
        List<Object> statementParameters = new LinkedList<>();

        if (lhsExpression != null && !lhsExpression.getType().equals("V")) {

            ClassName squareClassName = ClassName.bestGuess(lhsExpression.getType());

            if (!objectRoutine.getCreatedVariables().contains(lhsExpression.getName())) {
                statementBuilder.append("$T $L");
                statementParameters.add(squareClassName);
                statementParameters.add(lhsExpression.getName());
            } else {
                statementBuilder.append("$L");
                statementParameters.add(lhsExpression.getName());
            }

            statementBuilder.append(" = ");

        }

        if (expression.getMethodName().equals("<init>")) {
            statementBuilder.append("new $T(").append(parameterString).append(")");
            statementParameters.add(ClassName.bestGuess(expression.getReturnValue().getType()));
        } else {
            statementBuilder.append("$L.$L(").append(parameterString).append(")");
            statementParameters.add(expression.getSubject().getName());
            statementParameters.add(expression.getMethodName());
        }

        objectRoutine.getCreatedVariables().add(lhsExpression);

        objectRoutine.addStatement(statementBuilder.toString(), statementParameters);
    }

    public PendingStatement fromRecordedValue() {

        Parameter gson = new Parameter();
        gson.setName("gson");
        Parameter parameterValue = new Parameter();
        parameterValue.setValue(new String(lhsExpression.getProb().getSerializedValue()));
        Parameter parameterType = new Parameter();
        parameterType.setValue(lhsExpression.getType() + ".class");
        this.expression = new MethodCallExpression(
                "fromJson", gson, VariableContainer.from(List.of(parameterValue, parameterType)),
                this.lhsExpression, null
        );

        return this;
    }
}
