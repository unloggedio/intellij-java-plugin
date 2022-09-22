package com.insidious.plugin.factory.testcase.candidate;

import com.insidious.plugin.factory.testcase.expression.Expression;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;



public class TestCandidateMetadata {
    private List<MethodCallExpression> methodCallExpressions = new LinkedList<>();
    private VariableContainer fields = new VariableContainer();
    private Expression mainMethod;
    private Parameter testSubject;
    private long callTimeNanoSecond;
    private int entryProbeIndex;
    private int exitProbeIndex;
    private VariableContainer variables = new VariableContainer();
    private List<MethodCallExpression> staticCalls = new LinkedList<>();

    public int getExitProbeIndex() {
        return exitProbeIndex;
    }

    public void setExitProbeIndex(int exitProbeIndex) {
        this.exitProbeIndex = exitProbeIndex;
    }

    public Expression getMainMethod() {
        return mainMethod;
    }

    public void setMainMethod(Expression mainMethod) {
        this.mainMethod = mainMethod;
    }

    public VariableContainer getFields() {
        return fields;
    }

    public void setFields(VariableContainer fields) {
        this.fields = fields;
    }

    public void addAllFields(VariableContainer fieldsContainer) {
        fieldsContainer.all().forEach(this.fields::add);
    }

    public void setCallList(List<MethodCallExpression> callsList) {
        this.methodCallExpressions = callsList;
    }

    public String getFullyQualifiedClassname() {
        if (testSubject != null && testSubject.getType() != null) {
            return testSubject.getType();
        }
        return ((MethodCallExpression) getMainMethod()).getSubject().getType();
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

    public Collection<MethodCallExpression> getCallsList() {
        return methodCallExpressions;
    }

    @Override
    public String toString() {
        return "TestCandidateMetadata{" +
                "mainMethod=" + mainMethod +
                ", testSubject=" + testSubject +
                ", methodCallExpressions=" + methodCallExpressions +
                ", fields=" + fields +
                ", callTimeNanoSecond=" + callTimeNanoSecond +
                ", entryProbeIndex=" + entryProbeIndex +
                ", exitProbeIndex=" + exitProbeIndex +
                ", variables=" + variables +
                ", staticCalls=" + staticCalls +
                '}';
    }

    public int getEntryProbeIndex() {
        return entryProbeIndex;
    }

    public void setEntryProbeIndex(int entryProbeIndex) {
        this.entryProbeIndex = entryProbeIndex;
    }

    public VariableContainer getVariables() {
        return variables;
    }

    public void setVariables(VariableContainer variables) {
        this.variables = variables;
    }

    public List<MethodCallExpression> getStaticCalls() {
        return staticCalls;
    }

    public void setStaticCalls(List<MethodCallExpression> staticCalls) {
        this.staticCalls = staticCalls;
    }
}
