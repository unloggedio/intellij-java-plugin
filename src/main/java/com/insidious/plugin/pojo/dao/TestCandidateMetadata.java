package com.insidious.plugin.pojo.dao;

import com.intellij.openapi.util.text.Strings;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@DatabaseTable(tableName = "test_candidate")
public class TestCandidateMetadata {
    @DatabaseField
    private String methodCallExpressions;
    @DatabaseField
    private String fields;
    @DatabaseField(foreign = true, index = true)
    private MethodCallExpression mainMethod;
    @DatabaseField(foreign = true, index = true)
    private Parameter testSubject;
    @DatabaseField
    private long callTimeNanoSecond;
    @DatabaseField(id = true)
    private int entryProbeIndex;
    @DatabaseField
    private int exitProbeIndex;
    @DatabaseField
    private String variables;

    public static TestCandidateMetadata FromTestCandidateMetadata(com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata testCandidateMetadata) {
        TestCandidateMetadata newCandidate = new TestCandidateMetadata();

        newCandidate.setCallList(testCandidateMetadata.getCallsList().stream().map(
                com.insidious.plugin.pojo.MethodCallExpression::getId).collect(Collectors.toList()));

        newCandidate.setFields(testCandidateMetadata.getFields().all().stream()
                .map(e -> (long) e.getValue()).collect(Collectors.toList()));

        newCandidate.setTestSubject(Parameter.fromParameter(testCandidateMetadata.getTestSubject()));

        newCandidate.setMainMethod(MethodCallExpression.FromMCE((com.insidious.plugin.pojo.MethodCallExpression)
                testCandidateMetadata.getMainMethod()));

//        newCandidate.setVariables(testCandidateMetadata.getVariables().all()
//                .stream().map(e -> (long) e.getValue()).collect(Collectors.toList()));

        newCandidate.setCallTimeNanoSecond(testCandidateMetadata.getCallTimeNanoSecond());
        newCandidate.setEntryProbeIndex(Math.toIntExact(testCandidateMetadata.getEntryProbeIndex()));
        newCandidate.setExitProbeIndex(Math.toIntExact(testCandidateMetadata.getExitProbeIndex()));


        return newCandidate;
    }

    public static com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata toTestCandidate(TestCandidateMetadata testCandidateMetadata) {
        com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata newCandidate = new com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata();

        newCandidate.setExitProbeIndex(newCandidate.getExitProbeIndex());
        newCandidate.setCallTimeNanoSecond(newCandidate.getCallTimeNanoSecond());
        newCandidate.setEntryProbeIndex(newCandidate.getEntryProbeIndex());
        newCandidate.setExitProbeIndex(newCandidate.getExitProbeIndex());


        newCandidate.setCallTimeNanoSecond(testCandidateMetadata.getCallTimeNanoSecond());
        newCandidate.setEntryProbeIndex(testCandidateMetadata.getEntryProbeIndex());
        newCandidate.setExitProbeIndex(testCandidateMetadata.getExitProbeIndex());


        return newCandidate;
    }

    public int getExitProbeIndex() {
        return exitProbeIndex;
    }

    public void setExitProbeIndex(int exitProbeIndex) {
        this.exitProbeIndex = exitProbeIndex;
    }

    public com.insidious.plugin.pojo.dao.MethodCallExpression getMainMethod() {
        return mainMethod;
    }

    public void setMainMethod(com.insidious.plugin.pojo.dao.MethodCallExpression mainMethod) {
        this.mainMethod = mainMethod;
    }

    public List<Long> getFields() {
        if (fields == null || fields.length() < 1) {
            return List.of();
        }
        return Arrays.stream(fields.split(",")).map(Long::valueOf).collect(Collectors.toList());
    }

    public void setFields(List<Long> fields) {
        this.fields = Strings.join(fields, ",");
    }

    public void setCallList(List<Long> callsList) {
        this.methodCallExpressions = Strings.join(callsList, ",");
    }

    public String getFullyQualifiedClassname() {
        return getMainMethod().getSubject().getType();
    }

    public Parameter getTestSubject() {
        return testSubject;
    }

    public void setTestSubject(Parameter testSubject) {
        this.testSubject = testSubject;
    }

    public long getCallTimeNanoSecond() {
        return callTimeNanoSecond;
    }

    public void setCallTimeNanoSecond(long callTimeNanoSecond) {
        this.callTimeNanoSecond = callTimeNanoSecond;
    }

    public List<Long> getCallsList() {
        if (methodCallExpressions == null || methodCallExpressions.length() < 1) {
            return List.of();
        }
        return Arrays.stream(methodCallExpressions.split(",")).map(Long::valueOf).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "TCM[" + entryProbeIndex + " - " + exitProbeIndex + "]{" +
                "mainMethod=" + mainMethod +
                ", testSubject=" + testSubject +
                ", methodCallExpressions=" + methodCallExpressions +
                ", fields=" + fields +
                ", callTimeNanoSecond=" + callTimeNanoSecond +
                ", variables=" + variables +
                '}';
    }

    public int getEntryProbeIndex() {
        return entryProbeIndex;
    }

    public void setEntryProbeIndex(int entryProbeIndex) {
        this.entryProbeIndex = entryProbeIndex;
    }

    public List<Long> getVariables() {
        return Arrays.stream(variables.split(",")).map(Long::valueOf).collect(Collectors.toList());
    }

    public void setVariables(List<Long> variables) {
        this.variables = Strings.join(variables, ",");
    }
}

