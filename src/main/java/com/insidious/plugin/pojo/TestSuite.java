package com.insidious.plugin.pojo;

import java.util.List;

public class TestSuite {
    public final List<TestCaseScript> testCaseScripts;


    public TestSuite(List<TestCaseScript> testCaseScripts) {
        this.testCaseScripts = testCaseScripts;
    }

    public List<TestCaseScript> getTestCaseScripts() {
        return testCaseScripts;
    }

    @Override
    public String toString() {
        return "TestSuite{" +
                "testCaseScripts=" + testCaseScripts +
                '}';
    }
}
