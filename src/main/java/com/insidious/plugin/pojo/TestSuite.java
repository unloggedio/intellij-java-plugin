package com.insidious.plugin.pojo;

import java.util.List;

public class TestSuite {
    public final List<TestCaseUnit> testCaseScripts;
    public TestSuite(List<TestCaseUnit> testCaseScripts) {
        this.testCaseScripts = testCaseScripts;
    }

    public List<TestCaseUnit> getTestCaseScripts() {
        return testCaseScripts;
    }

    @Override
    public String toString() {
        return "TestSuite{" +
                "testCaseScripts=" + testCaseScripts +
                '}';
    }
}
