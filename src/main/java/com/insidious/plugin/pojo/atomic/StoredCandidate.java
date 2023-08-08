package com.insidious.plugin.pojo.atomic;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.insidious.plugin.assertions.AssertionType;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.util.TestCandidateUtils;
import org.jetbrains.annotations.NotNull;
import static com.insidious.plugin.factory.InsidiousService.HOSTNAME;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StoredCandidate implements Comparable<StoredCandidate> {

    public void setTestAssertions(AtomicAssertion testAssertions) {
        this.testAssertions = testAssertions;
    }

    private AtomicAssertion testAssertions = null;
    private String candidateId;
    private String name;
    private String description;
    private List<String> methodArguments;
    private String returnValue;
    private boolean isException;
    private String returnValueClassname;
    private StoredCandidateMetadata metadata;
    private long entryProbeIndex;
    private byte[] probSerializedValue;

    private MethodUnderTest methodUnderTest;

    public StoredCandidate() {
    }

    public StoredCandidate(TestCandidateMetadata candidateMetadata) {
        this.setException(candidateMetadata.getMainMethod().getReturnValue().isException());
        byte[] serializedValue = candidateMetadata.getMainMethod().getReturnDataEvent().getSerializedValue();
        String returnValue = serializedValue.length > 0 ? new String(serializedValue) :
                String.valueOf(candidateMetadata.getMainMethod().getReturnDataEvent().getValue());
        this.setReturnValue(returnValue);
        this.setMethodArguments(TestCandidateUtils.buildArgumentValuesFromTestCandidate(candidateMetadata));
        this.setReturnValueClassname(candidateMetadata.getMainMethod().getReturnValue().getType());
        this.setMethod(MethodUnderTest.fromTestCandidateMetadata(candidateMetadata));
        this.setProbSerializedValue(candidateMetadata.getMainMethod().getReturnValue().getProb().getSerializedValue());
        this.setEntryProbeIndex(generateIdentifier(candidateMetadata));
        StoredCandidateMetadata metadata = new StoredCandidateMetadata();
        metadata.setHostMachineName(HOSTNAME);
        metadata.setRecordedBy(HOSTNAME);
        metadata.setTimestamp(candidateMetadata.getMainMethod().getEntryProbe().getRecordedAt());
        this.setMetadata(metadata);
    }

    @Override
    public int hashCode() {
        return Objects.requireNonNullElseGet(this.candidateId,
                () -> this.entryProbeIndex + "-" + this.metadata.getHostMachineName()).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof StoredCandidate)) {
            return false;
        }
        StoredCandidate otherCandidate = (StoredCandidate) obj;
        if (otherCandidate.getCandidateId() != null && this.getCandidateId() == null) {
            return false;
        }
        if (this.getCandidateId() != null && otherCandidate.getCandidateId() == null) {
            return false;
        }

        if (this.getCandidateId() != null && otherCandidate.getCandidateId() != null) {
            return this.getCandidateId().equals(otherCandidate.getCandidateId());
        }

        return this.entryProbeIndex == otherCandidate.getEntryProbeIndex();
    }

    @Override
    public int compareTo(@NotNull StoredCandidate o) {
        return Long.compare(this.metadata.getTimestamp(), o.metadata.getTimestamp());
    }

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getMethodArguments() {
        return methodArguments;
    }

    public void setMethodArguments(List<String> methodArguments) {
        this.methodArguments = methodArguments;
    }

    public String getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(String returnValue) {
        this.returnValue = returnValue;
    }

    public boolean isException() {
        return isException;
    }

    public void setException(boolean exception) {
        isException = exception;
    }

    public String getReturnValueClassname() {
        return returnValueClassname;
    }

    public void setReturnValueClassname(String returnValueClassname) {
        this.returnValueClassname = returnValueClassname;
    }

    public StoredCandidateMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(StoredCandidateMetadata metadata) {
        this.metadata = metadata;
    }


    public MethodUnderTest getMethod() {
        return methodUnderTest;
    }

    public void setMethod(MethodUnderTest methodUnderTest1) {
        this.methodUnderTest = methodUnderTest1;
    }

    public long getEntryProbeIndex() {
        return entryProbeIndex;
    }

    public void setEntryProbeIndex(long entryProbeIndex) {
        this.entryProbeIndex = entryProbeIndex;
    }

    @JsonIgnore
    public boolean isReturnValueIsBoolean() {
        return returnValueClassname != null && (returnValueClassname.equals("Z") || returnValueClassname.equals(
                "java.lang.Boolean"));
    }

    public byte[] getProbSerializedValue() {
        return probSerializedValue;
    }

    public void setProbSerializedValue(byte[] probSerializedValue) {
        this.probSerializedValue = probSerializedValue;
    }

    @Override
    public String toString() {
        return "StoredCandidate{" +
                "candidateId='" + candidateId + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", methodArguments=" + methodArguments +
                ", returnValue='" + returnValue + '\'' +
                ", isException=" + isException +
                ", returnValueClassname='" + returnValueClassname + '\'' +
                ", metadata=" + metadata +
                ", method=" + methodUnderTest +
                ", entryProbeIndex=" + entryProbeIndex +
                '}';
    }

    private long generateIdentifier(TestCandidateMetadata candidateMetadata) {
        String id = "" + candidateMetadata.getMainMethod().getEntryProbe().getRecordedAt()
                + candidateMetadata.getEntryProbeIndex();
        return id.hashCode();
    }

    public void copyFrom(StoredCandidate candidate) {
        this.setCandidateId(candidate.getCandidateId());
        this.setName(candidate.getName());
        this.setDescription(candidate.getDescription());
        this.setReturnValue(candidate.getReturnValue());
        this.setEntryProbeIndex(candidate.getEntryProbeIndex());
        this.setProbSerializedValue(candidate.getProbSerializedValue());
        this.setException(candidate.isException());
        this.setReturnValueClassname(candidate.getReturnValueClassname());
    }

    public AtomicAssertion getTestAssertions() {
        return testAssertions;
    }
}
