package com.insidious.plugin.ui.Components.AtomicRecord;

import java.util.ArrayList;
import java.util.List;

public class AssertionBlockModel {
    private int id;
    //components reference
    private List<AssertionRule> ruleList = new ArrayList<>();
    //ruledata for loading
    private List<RuleData> ruleDataList;

    public AssertionBlockModel(int id, List<RuleData> ruleDataList) {
        this.id = id;
        this.ruleDataList = ruleDataList;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<AssertionRule> getElementList() {
        return ruleList;
    }

    public void setElementList(List<AssertionRule> ruleList) {
        this.ruleList = ruleList;
    }

    @Override
    public String toString() {
        return "AssertionBlockModel{" +
                "id=" + id +
                ", ruleList=" + ruleList +
                '}';
    }

    public List<RuleData> getRuleDataList() {
        return ruleDataList;
    }

    public void setRuleDataList(List<RuleData> ruleDataList) {
        this.ruleDataList = ruleDataList;
    }
}
