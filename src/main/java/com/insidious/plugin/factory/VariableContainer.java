package com.insidious.plugin.factory;

import com.insidious.plugin.pojo.Parameter;

import java.util.*;
import java.util.stream.Collectors;

public class VariableContainer {
    private final List<Parameter> parameterList = new LinkedList<>();

    public VariableContainer clone() {
        VariableContainer newContainer = new VariableContainer();
        for (Parameter parameter : this.parameterList) {
            newContainer.add(parameter);
        }
        return newContainer;
    }

    public void add(Parameter parameter) {
        this.parameterList.add(parameter);
    }

    public Parameter getParameterByName(String name) {
        for (Parameter parameter : this.parameterList) {
            if (Objects.equals(parameter.getName(), name)) {
                return parameter;
            }
        }
        return null;
    }

    public List<Parameter> getParametersByType(String typename) {
        return this.parameterList
                .stream()
                .filter(e -> e.getType().equals(typename))
                .collect(Collectors.toList());
    }

    public Optional<Parameter> getParametersById(String id) {
        return this.parameterList
                .stream()
                .filter(e -> e.getValue() != null && e.getValue().equals(id))
                .findFirst();
    }

    public boolean contains(String returnSubjectInstanceName) {
        return this.parameterList
                .stream()
                .anyMatch(e -> e.getName().equals(returnSubjectInstanceName));
    }

    public String createVariableName(String className) {
        if (className.contains(".")) {
            String[] classnamePart = className.split("\\.");
            className = classnamePart[classnamePart.length - 1];
        } else if (className.contains("/")) {
            String[] classnamePart = className.split("/");
            className = classnamePart[classnamePart.length - 1];
        }
        for (int i = 0; i < 100; i++) {
            String variableName = lowerInstanceName(className) + "Instance" + i;
            if (!contains(variableName)) {
                return variableName;
            }
        }
        throw new RuntimeException("could not generate name for the variable");
    }

    private static String upperInstanceName(String methodName) {
        return methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
    }

    private static String lowerInstanceName(String methodName) {
        int lowerIndex = 1;
        return methodName.substring(0, lowerIndex).toLowerCase() + methodName.substring(1);
    }

    public Collection<String> getNames() {
        return parameterList.stream().map(Parameter::getName).collect(Collectors.toList());
    }
}
