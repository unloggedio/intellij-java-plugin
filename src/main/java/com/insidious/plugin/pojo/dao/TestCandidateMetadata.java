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
    @Deprecated
    private String methodCallExpressions;
    @DatabaseField
    private String fields;
    @DatabaseField(index = true)
    private long mainMethod_id;
    @DatabaseField(index = true)
    private long testSubject_id;
    @DatabaseField
    private long callTimeNanoSecond;
    @DatabaseField(id = true)
    private int entryProbeIndex;
    @DatabaseField
    private int exitProbeIndex;
    @DatabaseField
    private String variables;

    public static TestCandidateMetadata FromTestCandidateMetadata(
            com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata testCandidateMetadata) {
        TestCandidateMetadata newCandidate = new TestCandidateMetadata();

        StringBuilder callIds = new StringBuilder();

        for (com.insidious.plugin.pojo.MethodCallExpression methodCallExpression : testCandidateMetadata.getCallsList()) {
            callIds.append(methodCallExpression.getId()).append(",");
        }

        String s = callIds.toString();
        if (s.length() > 0) {
            newCandidate.setCallList(s.substring(0, s.length() - 1));
        }
//        newCandidate.setCallList(testCandidateMetadata.getCallsList().stream().map(
//                com.insidious.plugin.pojo.MethodCallExpression::getId).collect(Collectors.toList()));

        newCandidate.setFields(testCandidateMetadata.getFields().all().stream()
                .map(com.insidious.plugin.pojo.Parameter::getValue).collect(Collectors.toList()));

        newCandidate.setTestSubject(Parameter.fromParameter(testCandidateMetadata.getTestSubject()));

        newCandidate.setMainMethod(MethodCallExpression.FromMCE((com.insidious.plugin.pojo.MethodCallExpression)
                testCandidateMetadata.getMainMethod()));


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

    public long getMainMethod() {
        return mainMethod_id;
    }

    public void setMainMethod(MethodCallExpression mainMethod) {
        this.mainMethod_id = mainMethod.getId();
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


    @Deprecated
    public void setCallList(String callIds) {
        this.methodCallExpressions = callIds;
    }

    public long getTestSubject() {
        return testSubject_id;
    }

    public void setTestSubject(Parameter testSubject) {
        if (testSubject == null) {
            return;
        }
        this.testSubject_id = testSubject.getValue();
    }

    public long getCallTimeNanoSecond() {
        return callTimeNanoSecond;
    }

    public void setCallTimeNanoSecond(long callTimeNanoSecond) {
        this.callTimeNanoSecond = callTimeNanoSecond;
    }


    @Deprecated
    public List<Long> getCallsList() {
        if (methodCallExpressions == null || methodCallExpressions.length() < 1) {
            return List.of();
        }
        return Arrays.stream(methodCallExpressions.split(",")).map(Long::valueOf).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "TCM[" + entryProbeIndex + " - " + exitProbeIndex + "]{" +
                "mainMethod=" + mainMethod_id +
                ", testSubject=" + testSubject_id +
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

