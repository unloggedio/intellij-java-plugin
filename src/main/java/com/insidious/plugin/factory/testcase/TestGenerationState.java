package com.insidious.plugin.factory.testcase;


import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.pojo.Parameter;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This container will hold the intermediate state of the script being generated, to keep track of
 * - created variables
 * - mocked calls
 */
public class TestGenerationState {
    private static final Gson gson = new Gson();
    private static final Pattern ENDS_WITH_DIGITS = Pattern.compile("(.+)([0-9]+)$");
    private VariableContainer variableContainer;
    private Map<String, Boolean> mockedCallsMap = new HashMap<>();
    private Map<String, JsonElement> valueResourceMap = new HashMap<>();
    private Map<String, String> valueResourceStringMap = new HashMap<>();

    public TestGenerationState() {
    }

    public VariableContainer getVariableContainer() {
        return variableContainer;
    }

    public void setVariableContainer(VariableContainer variableContainer) {
        this.variableContainer = variableContainer;
    }

    public Map<String, Boolean> getMockedCallsMap() {
        return mockedCallsMap;
    }

    public void setMockedCallsMap(Map<String, Boolean> mockedCallsMap) {
        this.mockedCallsMap = mockedCallsMap;
    }

    public Map<String, JsonElement> getValueResourceMap() {
        return valueResourceMap;
    }

    public String addObjectToResource(Parameter lhsExpression) {
        String targetObjectName = lhsExpression.getName();
        Matcher matcher = ENDS_WITH_DIGITS.matcher(targetObjectName);
        if (matcher.matches()) {
            targetObjectName = matcher.group(1);
        }

        String value = new String(lhsExpression.getProb().getSerializedValue());
        if (valueResourceStringMap.containsKey(value)) {
            valueResourceStringMap.get(value);
        }
        String referenceNameForValue = null;
        for (int i = 0; i < 100; i++) {
            referenceNameForValue = targetObjectName + i;
            if (!valueResourceMap.containsKey(referenceNameForValue)) {
                valueResourceMap.put(referenceNameForValue, gson.fromJson(value, JsonElement.class));
                valueResourceStringMap.put(value, referenceNameForValue);
                break;
            }
        }
        return referenceNameForValue;
    }
}
