package com.insidious.plugin.factory.testcase.expression;

import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;

public class ExpressionFactory {

    public static MethodCallExpression MethodCallExpression(String methodName, Parameter subjectParameter,
                                         VariableContainer from, Parameter callReturnParameter, Parameter exception) {
        return new MethodCallExpression(
                methodName, subjectParameter, from, callReturnParameter, exception
        );
    }
    public static Expression PlainValueExpression(String value) {
        return new PlainValueExpression(value);
    }
    public static Expression StringExpression(String s) {
        return new StringExpression(s);
    }
}
