package com.insidious.plugin.client;

import com.insidious.plugin.pojo.Parameter;
import org.apache.commons.lang.StringUtils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParameterNameFactory {

    private final Map<String, String> nameMap = new HashMap<>();
    private final Map<Parameter, String> parameterNameMapByObject = new HashMap<>();

    public String getNameForUse(Parameter parameter, String methodName) {
        if (parameterNameMapByObject.containsKey(parameter)) {
            return parameterNameMapByObject.get(parameter);
        }
        String key = parameter.getType() + "-" + parameter.getValue();
        String nameUsed = nameMap.get(key);
        if (nameUsed != null) {
            parameter.clearNames();
            parameter.setName(nameUsed);
            return nameUsed;
        }

        List<String> names = parameter.getNames();

        if (names.size() == 0) {
            return null;
        }

        if (names.size() == 1 || methodName == null || methodName.equals("") || methodName.equals("<init>")) {
            names.sort(Comparator.comparingInt(String::length));
            nameUsed = names.get(names.size() - 1);
        } else {
            nameUsed = getNameClosestToMethodName(names, methodName);
        }
        parameter.clearNames();
        parameter.setName(nameUsed);

        nameMap.put(key, nameUsed);

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
