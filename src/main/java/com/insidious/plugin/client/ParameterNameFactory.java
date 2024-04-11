package com.insidious.plugin.client;

import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.util.StringUtils;

import java.util.*;

public class ParameterNameFactory {

    private final Map<String, String> nameByIdMap = new HashMap<>();
    private final Map<String, Parameter> nameToParameterMap = new HashMap<>();
    private final Map<Parameter, String> parameterNameMapByObject = new HashMap<>();

    public String getNameForUse(Parameter parameter, String methodName) {
        if (parameterNameMapByObject.containsKey(parameter)) {
            return parameterNameMapByObject.get(parameter);
        } else if (parameter.getValue() != 0) {
            for (Map.Entry<Parameter, String> parameterStringEntry : parameterNameMapByObject.entrySet()) {
                if (parameterStringEntry.getKey().getValue() == parameter.getValue()) {
                    return parameterStringEntry.getValue();
                }
            }

        }
        String key = String.valueOf(parameter.getValue());
        if (parameter.getType() != null && parameter.getType().length() == 1) {
            key = parameter.getType() + "-" + parameter.getValue();
        }
        if ("0".equals(key)){
            if (parameter.getName() != null) {
                return parameter.getName();
            }
            return null;
        }

        String nameUsed = nameByIdMap.get(key);
        if (nameUsed != null) {
            parameter.clearNames();
            parameter.setName(nameUsed);
            return nameUsed;
        }

        List<String> names = new ArrayList<>(parameter.getNames());

        if (names.size() == 0) {
            return null;
        }
        List<String> namesAlreadyUsed = new ArrayList<>();
        for (String name : names) {
            if (nameToParameterMap.containsKey(name)) {
//                Parameter assignedParam = nameToParameterMap.get(name);
//                if (assignedParam.getValue() == parameter.getValue()) {
//                    return name;
//                }
                namesAlreadyUsed.add(name);
            }
        }
        if (namesAlreadyUsed.size() > 0) {
            names.removeAll(namesAlreadyUsed);
            for (String nameAlreadyUsed : namesAlreadyUsed) {
                int i = 1;
                while (true) {
                    if (!nameToParameterMap.containsKey(nameAlreadyUsed + i)) {
                        names.add(nameAlreadyUsed + i);
                        break;
                    }
                    i++;
                }
            }
        }

        if (names.size() == 0) {
            return null;
        }


        names.sort(Comparator.comparingInt(e -> -1 * e.length()));
        if (names.size() == 1 || methodName == null || methodName.equals("") || methodName.equals("<init>")) {
            nameUsed = getNameClosestToMethodName(names, parameter.getType());
        } else {
            nameUsed = getNameClosestToMethodName(names, methodName);
        }
        parameter.clearNames();
        parameter.setName(nameUsed);

        nameByIdMap.put(key, nameUsed);
        nameToParameterMap.put(nameUsed, parameter);

        return nameUsed;
    }

    private String getNameClosestToMethodName(List<String> names, String methodName) {
        int matchedDistance = 99999;

        String topName = "";
        if (names.size() > 0)
            topName = names.get(0);

        String matchedString = topName;
        if (methodName == null) {
            methodName = matchedString;
        }
        // select the string at least different from names[0]
        // and put it to the top
        for (String name : names) {
            int distance = StringUtils.getLevenshteinDistance(name, methodName);
            if (distance < matchedDistance) {
                matchedString = name;
                matchedDistance = distance;
            }
        }
        if (!matchedString.equals(topName)) {
            names.remove(matchedString);
            names.add(0, matchedString);
        }

        return matchedString;
    }

    public void setNameForParameter(Parameter returnSubjectExpectedObject, String expectedParameterName) {
        this.parameterNameMapByObject.put(returnSubjectExpectedObject, expectedParameterName);
    }

}
