package com.insidious.plugin.pojo.dao;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;


@DatabaseTable(tableName = "test_candidate")
public class TestCandidateMetadata {
    @DatabaseField(dataType = DataType.SERIALIZABLE)
    private Long[] methodCallExpressions = new Long[0];
    @DatabaseField(dataType = DataType.SERIALIZABLE)
    private Long[] fields = new Long[0];
    @DatabaseField(foreign = true)
    private MethodCallExpression mainMethod;
    @DatabaseField(foreign = true)
    private Parameter testSubject;
    @DatabaseField
    private long callTimeNanoSecond;
    @DatabaseField(id = true)
    private int entryProbeIndex;
    @DatabaseField
    private int exitProbeIndex;
    @DatabaseField(dataType = DataType.SERIALIZABLE)
    private Long[] variables = new Long[0];

    public static TestCandidateMetadata FromTestCandidateMetadata(com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata testCandidateMetadata) {
        TestCandidateMetadata newCandidate = new TestCandidateMetadata();
        newCandidate.setCallList(testCandidateMetadata.getCallsList().stream().map(com.insidious.plugin.pojo.MethodCallExpression::getEntryTime)
                .toArray(Long[]::new));
        newCandidate.setFields(testCandidateMetadata.getFields().all().stream()
                .map(e -> (long) e.getValue()).toArray(Long[]::new));
        newCandidate.setTestSubject(Parameter.fromParameter(testCandidateMetadata.getTestSubject()));
        newCandidate.setMainMethod(MethodCallExpression.FromMCE((com.insidious.plugin.pojo.MethodCallExpression) testCandidateMetadata.getMainMethod()));
        newCandidate.setVariables(testCandidateMetadata.getVariables().all()
                .stream().map(e -> (long) e.getValue()).toArray(Long[]::new));
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


//        newCandidate.setCallList(testCandidateMetadata.getCallsList()
//                .stream().map(com.insidious.plugin.pojo.MethodCallExpression::getEntryTime)
//                .toArray(Long[]::new));
//        newCandidate.setFields(testCandidateMetadata.getFields().all().stream()
//                .map(e -> (long) e.getValue()).toArray(Long[]::new));
//        newCandidate.setTestSubject(Parameter.fromParameter(testCandidateMetadata.getTestSubject()));
//        newCandidate.setMainMethod(MethodCallExpression.FromMCE((com.insidious.plugin.pojo.MethodCallExpression) testCandidateMetadata.getMainMethod()));
//        newCandidate.setVariables(testCandidateMetadata.getVariables().all()
//                .stream().map(e -> (long) e.getValue()).toArray(Long[]::new));


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

    public Long[] getFields() {
        return fields;
    }

    public void setFields(Long[] fields) {
        this.fields = fields;
    }

    public void setCallList(Long[] callsList) {
        this.methodCallExpressions = callsList;
    }

    public String getFullyQualifiedClassname() {
        return ((com.insidious.plugin.pojo.dao.MethodCallExpression) getMainMethod()).getSubject().getType();
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

    public Long[] getCallsList() {
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
                '}';
    }

    public int getEntryProbeIndex() {
        return entryProbeIndex;
    }

    public void setEntryProbeIndex(int entryProbeIndex) {
        this.entryProbeIndex = entryProbeIndex;
    }

    public Long[] getVariables() {
        return variables;
    }

    public void setVariables(Long[] variables) {
        this.variables = variables;
    }
}

