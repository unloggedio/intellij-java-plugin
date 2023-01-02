package com.insidious.plugin.factory.testcase;


import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;

import java.util.Base64;
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
    private Map<String, Object> valueResourceMap = new HashMap<>();
    private Map<String, String> valueResourceStringMap = new HashMap<>();
    private Logger logger = LoggerUtil.getInstance(TestGenerationState.class);

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

    public Map<String, Object> getValueResourceMap() {
        return valueResourceMap;
    }

    public String addObjectToResource(Parameter lhsExpression) {
        String targetObjectName = lhsExpression.getNameForUse(null);
        Matcher matcher = ENDS_WITH_DIGITS.matcher(targetObjectName);
        if (matcher.matches()) {
            targetObjectName = matcher.group(1);
        }

        String value = new String(lhsExpression.getProb()
                .getSerializedValue());
        if (valueResourceStringMap.containsKey(value)) {
            valueResourceStringMap.get(value);
        }
        String referenceNameForValue = null;
        for (int i = 0; i < 100; i++) {
            referenceNameForValue = targetObjectName + i;
            if (!valueResourceMap.containsKey(referenceNameForValue)) {
                Object value1 = null;
                try {
                    value1 = gson.fromJson(value, JsonElement.class);
                } catch (JsonSyntaxException jse) {
                    value1 = new String(lhsExpression.getProb().getSerializedValue());
                    logger.warn("Object was not serialized properly: " + value1 + " -> " + jse.getMessage());
                }
                valueResourceMap.put(referenceNameForValue, value1);
                valueResourceStringMap.put(value, referenceNameForValue);
                break;
            }
        }
        return referenceNameForValue;
    }
}
