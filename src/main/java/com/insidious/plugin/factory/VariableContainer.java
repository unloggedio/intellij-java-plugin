package com.insidious.plugin.factory;

import com.insidious.plugin.pojo.Parameter;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class VariableContainer {
    private final List<Parameter> parameterList = new LinkedList<>();


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
}
