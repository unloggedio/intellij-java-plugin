package com.insidious.plugin.ui.Components.AtomicRecord;

import java.util.List;

public class AssertionRulePayload {
    private String contextText = "AND";
    private List<String> keys;
    private List<String> compareTypes;
    private List<String> statuses;

    public AssertionRulePayload(String contextText, List<String> keys, List<String> compareTypes, List<String> statuses) {
        this.contextText = contextText;
        this.keys = keys;
        this.compareTypes = compareTypes;
        this.statuses = statuses;
    }

    public String getContextText() {
        return contextText;
    }

    public void setContextText(String contextText) {
        this.contextText = contextText;
    }

    public List<String> getKeys() {
        return keys;
    }

    public void setKeys(List<String> keys) {
        this.keys = keys;
    }

    public List<String> getCompareTypes() {
        return compareTypes;
    }

    public void setCompareTypes(List<String> compareTypes) {
        this.compareTypes = compareTypes;
    }

    public List<String> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<String> statuses) {
        this.statuses = statuses;
    }
}
