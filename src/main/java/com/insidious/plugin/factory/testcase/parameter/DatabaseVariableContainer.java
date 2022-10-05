package com.insidious.plugin.factory.testcase.parameter;

import com.insidious.plugin.client.DaoService;
import com.insidious.plugin.pojo.Parameter;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class DatabaseVariableContainer {
    final List<Parameter> parameterList = new LinkedList<>();
    final Map<Long, Parameter> parameterMap = new HashMap<>();
    private final DaoService daoService;
    private long variableContainerId;

    public DatabaseVariableContainer(DaoService daoService) {
        this.daoService = daoService;
    }

    public static String upperInstanceName(String methodName) {
        return methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
    }

    public static String lowerInstanceName(String methodName) {
        int lowerIndex = 1;
        return methodName.substring(0, lowerIndex).toLowerCase() + methodName.substring(1);
    }

    public static DatabaseVariableContainer from(List<Parameter> callArguments) {
        DatabaseVariableContainer variableContainer = new DatabaseVariableContainer(null);
        callArguments.forEach(variableContainer::add);
        return variableContainer;
    }

    public long getVariableContainerId() {
        return variableContainerId;
    }

    public void setVariableContainerId(long variableContainerId) {
        this.variableContainerId = variableContainerId;
    }

    public List<Parameter> getParameterList() {
        return parameterList;
    }

    public DatabaseVariableContainer clone() {
        DatabaseVariableContainer newContainer = new DatabaseVariableContainer(daoService);
        for (Parameter parameter : this.parameterList) {
            newContainer.add(parameter);
        }
        return newContainer;
    }

    public void add(Parameter parameter) {
        Object value = parameter.getValue();
        Parameter byValue = getParametersById(value);
        if (byValue == null) {
            this.parameterList.add(parameter);
            if (parameter.getProb() != null) {
                parameterMap.put(parameter.getProb().getValue(), parameter);
            }
            return;
        }
        if (byValue == parameter) {
            // literally the same value
            return;
        }
        if (parameter.getProb() != null) {

            if (byValue.getName() != null && byValue.getName().equals(parameter.getName())) {
                byte[] newSerializedValue = parameter.getProb().getSerializedValue();
                if (newSerializedValue == null || newSerializedValue.length == 0) {
                    return;
                }
                byte[] existingSerializedValue = byValue.getProb().getSerializedValue();
                if (existingSerializedValue == null || existingSerializedValue.length == 0) {
                    byValue.setProb(parameter.getProb());
                } else if (byValue.getProb().getNanoTime() < parameter.getProb().getNanoTime()) {
                    byValue.setProb(parameter.getProb());
                }
            } else {
                this.parameterList.add(parameter);
                parameterMap.put(parameter.getProb().getValue(), parameter);
            }


        }
    }

    public Parameter getParameterByName(String name) {
        for (Parameter parameter : this.parameterList) {
            if (parameter.hasName(name)) {
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

    public Parameter getParametersById(Object value) {
        Optional<Parameter> ret = this.parameterList
                .stream()
                .filter(e -> e.getValue() != null && e.getValue().equals(value))
                .findAny();
        return ret.orElse(null);
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

    public List<Parameter> all() {
        return parameterList;
    }

    public Parameter get(int i) {
        return parameterList.get(i);
    }

    public void normalize(DatabaseVariableContainer globalVariableContainer) {
        for (String name : getNames()) {
            Parameter localVariable = getParameterByName(name);
            if (globalVariableContainer.getParameterByName(localVariable.getName()) == null) {
                List<Parameter> byId = globalVariableContainer.getParametersByType(localVariable.getType());
                byId.forEach(e -> e.setName(localVariable.getName()));
//                byId.ifPresent(value -> value.setName(localVariable.getName()));
            }
        }
    }

    @Override
    public String toString() {
        return parameterList.size() + " Variables in {" + '}';
    }

    public Parameter getParameterByValue(long eventValue) {
        if (eventValue == 0) {
            return new Parameter(eventValue);
        }
        Parameter parameter = this.parameterMap.get(eventValue);
        if (parameter == null) {
            try {
                Parameter paramFromDatabase = daoService.getParameterByValue(eventValue);
                if (paramFromDatabase != null) {
                    parameterMap.put(eventValue, paramFromDatabase);
                    parameterList.add(paramFromDatabase);
                    return paramFromDatabase;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return new Parameter(eventValue);
            }
            parameter = new Parameter(eventValue);
        }
        return parameter;
    }
}
