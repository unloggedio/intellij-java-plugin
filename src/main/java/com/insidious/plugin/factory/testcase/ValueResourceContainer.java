package com.insidious.plugin.factory.testcase;

import java.util.HashMap;
import java.util.Map;

public class ValueResourceContainer {
    private final Map<String, Object> valueResourceMap = new HashMap<>();
    private final Map<String, String> valueResourceStringMap = new HashMap<>();
    private String resourceFileName;

    public ValueResourceContainer() {
    }

    public void setResourceFileName(String resourceFileName) {
        this.resourceFileName = resourceFileName;
    }

    public boolean containsHash(String valueHash) {
        return valueResourceStringMap.containsKey(valueHash);
    }

    public String getByHash(String valueHash) {
        return valueResourceStringMap.get(valueHash);
    }

    public void addValue(String referenceNameForValue, String valueHash, Object value1) {
        valueResourceMap.put(referenceNameForValue, value1);
        valueResourceStringMap.put(valueHash, referenceNameForValue);
    }

    public boolean containsName(String referenceNameForValue) {
        return valueResourceMap.containsKey(referenceNameForValue);
    }

    public Map<String, Object> getValueResourceMap() {
        return valueResourceMap;
    }

    public String getResourceFileName() {
        return resourceFileName;
    }
}
