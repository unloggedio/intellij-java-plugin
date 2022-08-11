package com.insidious.plugin.pojo;

import java.util.List;

public class MethodCallExpression {
    private final Parameter subject;
    private final List<Parameter> argument;
    private final Parameter returnValue;

    public MethodCallExpression(Parameter subject, List<Parameter> argument, Parameter returnValue) {
        this.subject = subject;
        this.argument = argument;
        this.returnValue = returnValue;
    }
}
