package com.insidious.plugin.factory.testcase;

import com.insidious.plugin.pojo.ObjectWithTypeInfo;
import com.insidious.plugin.pojo.Parameter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestCaseRequest {
    private final List<String> noMockClassList;
    private final Set<Long> dependentObjectList;
    private final List<ObjectWithTypeInfo> targetObjectList;
    private Parameter targetParameter;
    private int buildLevel = 0;

    public TestCaseRequest(
            List<ObjectWithTypeInfo> targetObjectList,
            List<String> noMockClassList,
            Set<Long> dependentObjectList) {
        this.targetObjectList = targetObjectList;
        this.noMockClassList = noMockClassList;
        this.dependentObjectList = new HashSet<>(dependentObjectList);
    }

    public Parameter getTargetParameter() {
        return targetParameter;
    }

    public void setTargetParameter(Parameter targetParameter) {
        this.targetParameter = targetParameter;
    }


    public static TestCaseRequest nextLevel(TestCaseRequest testCaseRequest) {
        TestCaseRequest testCaseRequest1 = new TestCaseRequest(testCaseRequest.getTargetObjectList(),
                testCaseRequest.getNoMockClassList(), testCaseRequest.getDependentObjectList());
        testCaseRequest1.setTargetParameter(testCaseRequest.getTargetParameter());
        testCaseRequest1.buildLevel += 1;
        return testCaseRequest1;
    }

    public Set<Long> getDependentObjectList() {
        return dependentObjectList;
    }

    public int getBuildLevel() {
        return buildLevel;
    }

    public void setBuildLevel(int buildLevel) {
        this.buildLevel = buildLevel;
    }

    public List<ObjectWithTypeInfo> getTargetObjectList() {
        return targetObjectList;
    }

    public List<String> getNoMockClassList() {
        return noMockClassList;
    }
}
