package com.insidious.plugin.factory.testcase;


import com.insidious.plugin.factory.testcase.parameter.VariableContainer;

import java.util.HashMap;
import java.util.Map;

/**
 * This container will hold the intermediate state of the script being generated, to keep track of
 *  - created variables
 *  - mocked calls
 */
public class TestGenerationState {
    private VariableContainer variableContainer;
    private Map<String, Boolean> mockedCallsMap = new HashMap<>();

    public void setVariableContainer(VariableContainer variableContainer) {
        this.variableContainer = variableContainer;
    }

    public VariableContainer getVariableContainer() {
        return variableContainer;
    }

    public Map<String, Boolean> getMockedCallsMap() {
        return mockedCallsMap;
    }

    public void setMockedCallsMap(Map<String, Boolean> mockedCallsMap) {
        this.mockedCallsMap = mockedCallsMap;
    }
}
