package com.insidious.plugin.factory.testcase;


import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.pojo.Parameter;

import java.util.HashMap;
import java.util.Map;

/**
 * This container will hold the intermediate state of the script being generated, to keep track of
 * - created variables
 * - mocked calls
 */
public class TestGenerationState {
    private VariableContainer variableContainer;
    private Map<String, Boolean> mockedCallsMap = new HashMap<>();

    private Map<String, JsonElement> valueResourceMap = new HashMap<>();
    private static final Gson gson = new Gson();

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
        String targetClassName = lhsExpression.getName();
        String value = new String(lhsExpression.getProb().getSerializedValue());
        if (valueResourceMap.containsValue(value)) {
            for (String s : valueResourceMap.keySet()) {
                if (valueResourceMap.get(s).equals(value)) {
                    return s;
                }
            }
        }
        String referenceNameForValue = null;
        for (int i = 0; i < 100; i++) {
            referenceNameForValue = targetClassName + i;
            if (!valueResourceMap.containsKey(referenceNameForValue)) {
                valueResourceMap.put(referenceNameForValue, gson.fromJson(value, JsonElement.class));
                break;
            }
        }
        return referenceNameForValue;
    }
}
