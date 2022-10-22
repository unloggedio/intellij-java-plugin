package com.insidious.plugin.factory.testcase.candidate;

import com.insidious.plugin.factory.testcase.expression.Expression;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;



public class TestCandidateMetadata {
    private List<MethodCallExpression> methodCallExpressions = new LinkedList<>();
    private VariableContainer fields = new VariableContainer();
    private Expression mainMethod;
    private Parameter testSubject;
    private long callTimeNanoSecond;
    private long entryProbeIndex;
    private long exitProbeIndex;
//    private VariableContainer variables = new VariableContainer();
    private List<MethodCallExpression> staticCalls = new LinkedList<>();

    public long getExitProbeIndex() {
        return exitProbeIndex;
    }

    public void setExitProbeIndex(long exitProbeIndex) {
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

    public List<MethodCallExpression> getCallsList() {
        return methodCallExpressions;
    }

    @Override
    public String toString() {
        return "TCM{" +
                 mainMethod +
                " calls " + methodCallExpressions.size() +
                " methods " +
                ", callTimeNanoSecond=" + callTimeNanoSecond +
                ", entryProbeIndex=" + entryProbeIndex +
                ", exitProbeIndex=" + exitProbeIndex +
                '}';
    }

    public long getEntryProbeIndex() {
        return entryProbeIndex;
    }

    public void setEntryProbeIndex(long entryProbeIndex) {
        this.entryProbeIndex = entryProbeIndex;
    }

//    public VariableContainer getVariables() {
//        return variables;
//    }

//    public void setVariables(VariableContainer variables) {
//        this.variables = variables;
//    }

    public List<MethodCallExpression> getStaticCalls() {
        return staticCalls;
    }

    public void setStaticCalls(List<MethodCallExpression> staticCalls) {
        this.staticCalls = staticCalls;
    }

    public void addMethodCall(MethodCallExpression topCall) {
        methodCallExpressions.add(topCall);
    }

    public void addAllMethodCall(Collection<MethodCallExpression> topCall) {
        methodCallExpressions.addAll(topCall);
    }
}
