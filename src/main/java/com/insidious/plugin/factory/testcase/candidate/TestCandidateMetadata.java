package com.insidious.plugin.factory.testcase.candidate;

import com.insidious.plugin.factory.testcase.expression.Expression;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


public class TestCandidateMetadata {
    private List<MethodCallExpression> methodCallExpressions = new LinkedList<>();
    private VariableContainer fields = new VariableContainer();
    private MethodCallExpression mainMethod;
    private Parameter testSubject;
    private long callTimeNanoSecond;
    private long entryProbeIndex;
    private long exitProbeIndex;
    private final List<TestAssertion> assertionList = new ArrayList<>();

    private boolean isUIselected = false;

    public boolean isUIselected() {
        return isUIselected;
    }

    public void setUIselected(boolean UIselected) {
        isUIselected = UIselected;
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
}
