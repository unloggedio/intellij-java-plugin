package com.insidious.plugin.pojo.dao;

import com.insidious.plugin.util.StringUtils;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.*;
import java.util.stream.Collectors;


@DatabaseTable(tableName = "test_candidate")
public class TestCandidateMetadata {
    private final Set<Long> fieldIds = new HashSet<>();
    private final Set<Integer> lineIds = new HashSet<>();
    @DatabaseField
    private String fields;
    @DatabaseField
    private String lines;
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
    private long createdAt = new Date().getTime();
    @DatabaseField
    private String variables;

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public static com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata
    toTestCandidate(TestCandidateMetadata testCandidateMetadata) {
        com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata newCandidate
                = new com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata();

        newCandidate.setEntryProbeIndex(testCandidateMetadata.getEntryProbeIndex());
        newCandidate.setExitProbeIndex(testCandidateMetadata.getExitProbeIndex());
        newCandidate.setCallTimeNanoSecond(testCandidateMetadata.getCallTimeNanoSecond());
        newCandidate.setCreatedAt(testCandidateMetadata.getCreatedAt());
        if (testCandidateMetadata.getLines() != null) {
            newCandidate.setLines(
                    new ArrayList<>(
                            Arrays
                                    .stream(testCandidateMetadata.getLines().split(","))
                                    .map(Integer::parseInt).collect(Collectors.toList())
                    )
            );
        }

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

    public Set<Long> getFields() {
        if (fields == null || fields.length() < 1) {
            return Collections.emptySet();
        }
        return Arrays.stream(fields.split(","))
                .map(Long::valueOf)
                .collect(Collectors.toSet());
    }

    public void setFields(List<Long> fields) {
        this.fields = StringUtils.join(fields, ",");
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


    @Override
    public String toString() {
        return "TCM[" + entryProbeIndex + " - " + exitProbeIndex + "]{" +
                "mainMethod=" + mainMethod_id +
                ", testSubject=" + testSubject_id +
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
        if (variables == null || variables.length() == 0) {
            return new ArrayList<>();
        }
        return Arrays.stream(variables.split(","))
                .filter(e -> e.length() > 0)
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    public void setVariables(List<Long> variables) {
        this.variables = StringUtils.join(variables, ",");
    }

    public void addField(long parameterValue) {
        if (fieldIds.contains(parameterValue)) {
            return;
        }
        fieldIds.add(parameterValue);
        if (fields == null || fields.length() == 0) {
            fields = String.valueOf(parameterValue);
        } else {
            fields = fields + "," + parameterValue;
        }
    }

    public void addLineCovered(int line) {
        if (lineIds.contains(line)) {
            return;
        }
        lineIds.add(line);
        if (lines == null || lines.length() == 0) {
            lines = String.valueOf(line);
        } else {
            lines = lines + "," + line;
        }

    }

    public String getLines() {
        return lines;
    }
}

