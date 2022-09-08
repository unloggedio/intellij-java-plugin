package com.insidious.plugin.pojo;

import com.insidious.plugin.factory.testcase.expression.Expression;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;

public class MethodCallExpression implements Expression {
    private Parameter subject;
    private final VariableContainer arguments;
    private final Parameter returnValue;
    private final String methodName;
    private final Parameter exception;

    public MethodCallExpression(
            String methodName,
            Parameter subject,
            VariableContainer arguments,
            Parameter returnValue,
            Parameter exception
    ) {
        this.methodName = methodName;
        this.subject = subject;
        this.arguments = arguments;
        this.returnValue = returnValue;
        this.exception = exception;
    }

    public Parameter getSubject() {
        return subject;
    }

    public VariableContainer getArguments() {
        return arguments;
    }

    public Parameter getReturnValue() {
        return returnValue;
    }

    public String getMethodName() {
        return methodName;
    }

    public Parameter getException() {
        return exception;
    }

    public void setSubject(Parameter testSubject) {
        this.subject = testSubject;
    }
}
