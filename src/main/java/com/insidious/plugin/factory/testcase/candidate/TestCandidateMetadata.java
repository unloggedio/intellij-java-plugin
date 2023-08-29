package com.insidious.plugin.factory.testcase.candidate;

import com.insidious.plugin.assertions.TestAssertion;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;


import java.util.*;
import java.util.stream.Collectors;


public class TestCandidateMetadata implements Comparable<TestCandidateMetadata> {
    private final List<TestAssertion> assertionList = new ArrayList<>();
    private List<MethodCallExpression> methodCallExpressions = new LinkedList<>();
    private VariableContainer fields = new VariableContainer();
    private MethodCallExpression mainMethod;
    private Parameter testSubject;
    private long callTimeNanoSecond;
    private long entryProbeIndex;
    private long exitProbeIndex;
    private List<Integer> lineNumbers;

    public TestCandidateMetadata(TestCandidateMetadata original) {

        assertionList.addAll(original.assertionList
                .stream().map(TestAssertion::new)
                .collect(Collectors.toList()));
        methodCallExpressions = original.methodCallExpressions
                .stream().map(MethodCallExpression::new)
                .collect(Collectors.toList());
        fields = (VariableContainer) original.fields.clone();
        mainMethod = new MethodCallExpression(original.mainMethod);
        testSubject = new Parameter(original.testSubject);
        callTimeNanoSecond = original.callTimeNanoSecond;
        entryProbeIndex = original.entryProbeIndex;
        exitProbeIndex = original.exitProbeIndex;
        lineNumbers = new ArrayList<>(original.lineNumbers);
    }

    public TestCandidateMetadata() {
    }

    public List<Integer> getLineNumbers() {
        return lineNumbers;
    }

    public long getExitProbeIndex() {
        return exitProbeIndex;
    }

    public void setExitProbeIndex(long exitProbeIndex) {
        this.exitProbeIndex = exitProbeIndex;
    }

    public MethodCallExpression getMainMethod() {
        return mainMethod;
    }

    public void setMainMethod(MethodCallExpression mainMethod) {
        this.mainMethod = mainMethod;
    }

    public VariableContainer getFields() {
        return fields;
    }

    public void setFields(VariableContainer fields) {
        this.fields = fields;
    }

    public void addAllFields(VariableContainer fieldsContainer) {
        fieldsContainer.all()
                .forEach(this.fields::add);
    }

    public void setCallList(List<MethodCallExpression> callsList) {
        this.methodCallExpressions = callsList;
    }

    public String getFullyQualifiedClassname() {
        if (testSubject != null && testSubject.getType() != null) {
            return testSubject.getType();
        }
        return mainMethod.getSubject().getType();
    }

    public Parameter getTestSubject() {
        return testSubject;
    }

    public void setTestSubject(Parameter testSubject) {
        this.testSubject = testSubject;
        if (this.mainMethod != null && this.mainMethod instanceof MethodCallExpression) {
            MethodCallExpression mce = (MethodCallExpression) this.mainMethod;
            mce.setSubject(testSubject);
        }
    }


    public long getCallTimeNanoSecond() {
        return callTimeNanoSecond;
    }

    public void setCallTimeNanoSecond(long callTimeNanoSecond) {
        this.callTimeNanoSecond = callTimeNanoSecond;
    }

    public List<MethodCallExpression> getCallsList() {
        return methodCallExpressions;
    }

    @Override
    public String toString() {
        return mainMethod.toString();
    }

    public long getEntryProbeIndex() {
        return entryProbeIndex;
    }

    public void setEntryProbeIndex(long entryProbeIndex) {
        this.entryProbeIndex = entryProbeIndex;
    }

    public void addMethodCall(MethodCallExpression topCall) {
        methodCallExpressions.add(topCall);
    }

    @Deprecated
    public void addAllMethodCall(Collection<MethodCallExpression> topCall) {
        methodCallExpressions.addAll(topCall);
    }

    public List<TestAssertion> getAssertionList() {
        return assertionList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestCandidateMetadata that = (TestCandidateMetadata) o;
        return entryProbeIndex == that.entryProbeIndex && exitProbeIndex == that.exitProbeIndex && testSubject.equals(
                that.testSubject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testSubject, entryProbeIndex, exitProbeIndex);
    }

    @Override
    public int compareTo( TestCandidateMetadata o) {
        return Long.compare(this.entryProbeIndex, o.entryProbeIndex);
    }

    public void setLines(List<Integer> lineNumbers) {
        this.lineNumbers = lineNumbers;
    }
}
