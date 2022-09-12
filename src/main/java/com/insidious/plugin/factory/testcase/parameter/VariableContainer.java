package com.insidious.plugin.factory.testcase.parameter;

import com.insidious.plugin.pojo.Parameter;

import java.util.*;
import java.util.stream.Collectors;

public class VariableContainer {
    private final List<Parameter> parameterList = new LinkedList<>();

    public static String upperInstanceName(String methodName) {
        return methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
    }

    public static String lowerInstanceName(String methodName) {
        int lowerIndex = 1;
        return methodName.substring(0, lowerIndex).toLowerCase() + methodName.substring(1);
    }

    public static VariableContainer from(List<Parameter> callArguments) {
        VariableContainer variableContainer = new VariableContainer();
        callArguments.forEach(variableContainer::add);
        return variableContainer;
    }

    public VariableContainer clone() {
        VariableContainer newContainer = new VariableContainer();
        for (Parameter parameter : this.parameterList) {
            newContainer.add(parameter);
        }
        return newContainer;
    }

    public void add(Parameter parameter) {
        Object value = parameter.getValue();
        Optional<Parameter> byId = getParametersById(String.valueOf(value));
        if (Objects.equals(value, "0") || byId.isEmpty()) {
            this.parameterList.add(parameter);
        } else if (parameter.getProb() != null) {
            byte[] newSerializedValue = parameter.getProb().getSerializedValue();
            if (newSerializedValue == null || newSerializedValue.length == 0) {
                return;
            }
            Parameter existing = byId.get();
            byte[] existingSerializedValue = existing.getProb().getSerializedValue();
            if (existingSerializedValue == null || existingSerializedValue.length == 0) {
                existing.setProb(parameter.getProb());
            } else if (existing.getProb().getNanoTime() < parameter.getProb().getNanoTime()) {
                existing.setProb(parameter.getProb());
            }
        }
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

    public boolean contains(String variableName) {
        return this.parameterList.stream()
                .anyMatch(e -> e.getName() != null &&
                        e.getName().equals(variableName));
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

    public Collection<String> getNames() {
        return parameterList.stream().map(Parameter::getName).collect(Collectors.toList());
    }

    public int count() {
        return parameterList.size();
    }

    public List<? extends Parameter> all() {
        return parameterList;
    }

    public Parameter get(int i) {
        return parameterList.get(i);
    }

    public void normalize(VariableContainer globalVariableContainer) {
        for (String name : getNames()) {
            Parameter localVariable = getParameterByName(name);
            if (globalVariableContainer.getParameterByName(localVariable.getName()) == null) {
                Optional<Parameter> byId = globalVariableContainer.getParametersById(localVariable.getType());
                byId.ifPresent(value -> value.setName(localVariable.getName()));
            }
        }
    }

    @Override
    public String toString() {
        return parameterList.size() + " Variables in {" + '}';
    }
}
