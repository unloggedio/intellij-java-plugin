package com.insidious.plugin.factory.testcase.parameter;

import com.insidious.common.weaver.TypeInfo;
import com.insidious.plugin.client.DaoService;
import com.insidious.plugin.client.cache.ArchiveIndex;
import com.insidious.plugin.pojo.Parameter;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class DatabaseVariableContainer {
    final List<Parameter> parameterList = new LinkedList<>();
    final Map<Long, Parameter> parameterMap = new HashMap<>();
    private final DaoService daoService;
    private final ArchiveIndex archiveIndex;
    private long variableContainerId;
    private final Map<Long, Boolean> ensuredParameters = new HashMap<>();
    private final List<Parameter> beingSaved = Collections.synchronizedList(new ArrayList<>());

    public DatabaseVariableContainer(DaoService daoService, ArchiveIndex archiveIndex) {
        this.daoService = daoService;
        this.archiveIndex = archiveIndex;
    }

    public static String upperInstanceName(String methodName) {
        return methodName.substring(0, 1)
                .toUpperCase() + methodName.substring(1);
    }

    public static String lowerInstanceName(String methodName) {
        int lowerIndex = 1;
        return methodName.substring(0, lowerIndex)
                .toLowerCase() + methodName.substring(1);
    }

    public static DatabaseVariableContainer from(List<Parameter> callArguments) {
        DatabaseVariableContainer variableContainer = new DatabaseVariableContainer(null, null);
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
        DatabaseVariableContainer newContainer = new DatabaseVariableContainer(daoService, archiveIndex);
        for (Parameter parameter : this.parameterList) {
            newContainer.add(parameter);
        }
        return newContainer;
    }

    public void add(Parameter parameter) {
        long value = parameter.getValue();
        Parameter byValue = parameterMap.get(value);
        if (byValue == null) {
            this.parameterList.add(parameter);
            if (parameter.getProb() != null) {
                parameterMap.put(parameter.getProb()
                        .getValue(), parameter);
            }
            return;
        }
        if (byValue == parameter) {
            // literally the same value
            return;
        }
        if (parameter.getProb() != null) {

//            if (byValue.getName() != null && byValue.getName().equals(parameter.getName())) {
//                byte[] newSerializedValue = parameter.getProb().getSerializedValue();
//                if (newSerializedValue == null || newSerializedValue.length == 0) {
//                    return;
//                }
//                byte[] existingSerializedValue = byValue.getProb().getSerializedValue();
//                if (existingSerializedValue == null || existingSerializedValue.length == 0) {
//                    byValue.setProb(parameter.getProb());
//                } else if (byValue.getProb().getNanoTime() < parameter.getProb().getNanoTime()) {
//                    byValue.setProb(parameter.getProb());
//                }
//            } else {
            this.parameterList.add(parameter);
            parameterMap.put(parameter.getProb()
                    .getValue(), parameter);
//            }

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
                .filter(e -> e.getType()
                        .equals(typename))
                .collect(Collectors.toList());
    }

    public Parameter getParametersById(long value) {
        Parameter ret = this.parameterMap.get(value);
        return ret;
    }

    public boolean contains(String variableName) {
        return this.parameterList.stream()
                .anyMatch(e -> e.getName() != null &&
                        e.getName()
                                .equals(variableName));
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
        return parameterList.stream()
                .map(Parameter::getName)
                .collect(Collectors.toList());
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

                synchronized (beingSaved) {
                    Optional<Parameter> isBeingSaved = beingSaved.stream()
                            .filter(e -> e.getValue() == eventValue)
                            .findFirst();
                    if (isBeingSaved.isPresent()) {
                        Parameter e = isBeingSaved.get();
                        parameterList.add(e);
                        parameterMap.put(e.getValue(), e);
                        return e;
                    }
                }

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
            return parameter;
        }
        return parameter;
    }

    public void ensureParameterType(Parameter existingParameter, String expectingClassName) {
        if (existingParameter.getValue() < 10000 || existingParameter.getType() == null) {
            existingParameter.setType(expectingClassName);
            return;
        }
        if (ensuredParameters.containsKey(existingParameter.getValue())) {
            return;
        }
        ensuredParameters.put(existingParameter.getValue(), true);
        if (expectingClassName.length() < 2 || expectingClassName.endsWith("]")) {
            return;
        }
        if (expectingClassName.equals("java.lang.Object")) {
            return;
        }
        TypeInfo parameterType = archiveIndex.getObjectType(existingParameter.getValue());
        if (parameterType == null) {
            return;
        }
        Map<String, TypeInfo> typeHierarchy = archiveIndex.getTypesById(Set.of((int) parameterType.getTypeId()));
        TypeInfo expectedTypeInfo = archiveIndex.getTypesByName(expectingClassName);
        if (expectedTypeInfo == null) {
            // this is the case of some generated class name containing $$
            // and stuff (array types we have excluded earlier) like a proxy object
            // selecting the right type gets very tricky
            if (!parameterType.getTypeNameFromClass()
                    .contains("$")) {
                throw new RuntimeException("this one has no $$$");
            }
            return;
        }
        if (!typeHierarchy.containsKey(String.valueOf(expectedTypeInfo.getTypeId()))) {
            // the type info we got from the probe is probably wrong
            return;
        }
        if (parameterType.getTypeId() < expectedTypeInfo.getTypeId()) {
            existingParameter.setType(expectedTypeInfo.getTypeNameFromClass());
        }
    }

    public void ensureParameterType(Parameter existingParameter, TypeInfo parameterType) {
        if (existingParameter.getType() == null) {
            existingParameter.setType(parameterType.getTypeNameFromClass());
            return;
        }
        if (existingParameter.getValue() < 10000) {
//            existingParameter.setType(expectingClassName);
            return;
        }
        if (ensuredParameters.containsKey(existingParameter.getValue())) {
            return;
        }
        ensuredParameters.put(existingParameter.getValue(), true);
        if (existingParameter.getType()
                .length() < 2 || existingParameter.getType()
                .endsWith("]")) {
            return;
        }
        existingParameter.setType(parameterType.getTypeNameFromClass());

    }

    public List<Parameter> getBeingSaved() {
        return beingSaved;
    }

    public <E> void setBeingSaved(List<Parameter> beingSaved) {
        synchronized (this.beingSaved) {
            this.beingSaved.addAll(beingSaved);
        }
    }

}
