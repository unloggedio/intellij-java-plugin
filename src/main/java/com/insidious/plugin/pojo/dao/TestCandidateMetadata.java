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
    private long entryProbeIndex;
    @DatabaseField
    private long exitProbeIndex;
    @DatabaseField
    private String variables;

    public static TestCandidateMetadata FromTestCandidateMetadata(
            com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata testCandidateMetadata) {
        TestCandidateMetadata newCandidate = new TestCandidateMetadata();

        StringBuilder callIds = new StringBuilder();

        for (com.insidious.plugin.pojo.MethodCallExpression methodCallExpression : testCandidateMetadata.getCallsList()) {
            callIds.append(methodCallExpression.getId())
                    .append(",");
        }

        String s = callIds.toString();
        if (s.length() > 0) {
            newCandidate.setCallList(s.substring(0, s.length() - 1));
        }

        newCandidate.setFields(testCandidateMetadata.getFields()
                .all()
                .stream()
                .map(com.insidious.plugin.pojo.Parameter::getValue)
                .collect(Collectors.toList()));

        if (testCandidateMetadata.getTestSubject() != null) {
            newCandidate.setTestSubject(testCandidateMetadata.getTestSubject()
                    .getValue());
        }

        newCandidate.setMainMethod(((com.insidious.plugin.pojo.MethodCallExpression)
                testCandidateMetadata.getMainMethod()).getId());


        newCandidate.setCallTimeNanoSecond(testCandidateMetadata.getCallTimeNanoSecond());
        newCandidate.setEntryProbeIndex(Math.toIntExact(testCandidateMetadata.getEntryProbeIndex()));
        newCandidate.setExitProbeIndex(Math.toIntExact(testCandidateMetadata.getExitProbeIndex()));


        return newCandidate;
    }

    public static com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata
    toTestCandidate(TestCandidateMetadata testCandidateMetadata) {
        com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata newCandidate
                = new com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata();

        newCandidate.setEntryProbeIndex(testCandidateMetadata.getEntryProbeIndex());
        newCandidate.setExitProbeIndex(testCandidateMetadata.getExitProbeIndex());
        newCandidate.setCallTimeNanoSecond(testCandidateMetadata.getCallTimeNanoSecond());

        return newCandidate;
    }

    public long getExitProbeIndex() {
        return exitProbeIndex;
    }

    public void setExitProbeIndex(long exitProbeIndex) {
        this.exitProbeIndex = exitProbeIndex;
    }

    public long getMainMethod() {
        return mainMethod_id;
    }

    public void setMainMethod(Long methodId) {
        this.mainMethod_id = methodId;
    }

    public List<Long> getFields() {
        if (fields == null || fields.length() < 1) {
            return List.of();
        }
        return Arrays.stream(fields.split(","))
                .map(Long::valueOf)
                .collect(Collectors.toList());
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

    public void setTestSubject(Long subjectId) {
        this.testSubject_id = subjectId;
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
        return Arrays.stream(methodCallExpressions.split(","))
                .map(Long::valueOf)
                .collect(Collectors.toList());
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

    public long getEntryProbeIndex() {
        return entryProbeIndex;
    }

    public void setEntryProbeIndex(long entryProbeIndex) {
        this.entryProbeIndex = entryProbeIndex;
    }

    public List<Long> getVariables() {
        return Arrays.stream(variables.split(","))
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    public void setVariables(List<Long> variables) {
        this.variables = Strings.join(variables, ",");
    }

    public void addField(long parameterValue) {
        if (fields == null || fields.length() == 0) {
            fields = String.valueOf(parameterValue);
        } else {
            fields = fields + "," + parameterValue;
        }
    }
}

