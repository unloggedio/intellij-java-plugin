package com.insidious.plugin.pojo;

import java.util.List;

public class MethodCallExpression {
    private final Parameter subject;
    private final List<Parameter> arguments;
    private final Parameter returnValue;
    private final String methodName;

    public MethodCallExpression(String methodName,
                                Parameter subject, List<Parameter> arguments, Parameter returnValue) {
        this.methodName = methodName;
        this.subject = subject;
        this.arguments = arguments;
        this.returnValue = returnValue;
    }

    public Parameter getSubject() {
        return subject;
    }

    public List<Parameter> getArguments() {
        return arguments;
    }

    public Parameter getReturnValue() {
        return returnValue;
    }

    public String getMethodName() {
        return methodName;
    }
}
