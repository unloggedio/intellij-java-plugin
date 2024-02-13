package com.insidious.plugin.pojo.atomic;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.util.TestCandidateUtils;

import java.util.*;

import static com.insidious.plugin.Constants.HOSTNAME;

public class StoredCandidate implements Comparable<StoredCandidate> {

    @JsonIgnore
    private long entryProbeIndex;
    private List<Integer> lineNumbers = new ArrayList<>();
    private AtomicAssertion testAssertions = null;
    private String candidateId;
    private String name;
    private String description;
    private List<String> methodArguments;
    private String returnValue;
    private boolean isException;
    private String returnValueClassname;
    private StoredCandidateMetadata metadata;
    private long sessionIdentifier;
    private byte[] probSerializedValue;
    private MethodUnderTest methodUnderTest;
    private Set<String> mockIds = new HashSet<String>();
    private long createdAt;

    private StoredCandidate() {
    }

    public StoredCandidate(TestCandidateMetadata candidateMetadata) {
        MethodCallExpression mainMethod = candidateMetadata.getMainMethod();
        Parameter returnValue1 = mainMethod.getReturnValue();
        this.setException(returnValue1.isException());
        byte[] serializedValue = candidateMetadata.getMainMethod().getReturnDataEvent().getSerializedValue();
        this.returnValue = serializedValue.length > 0 ? new String(serializedValue) :
                String.valueOf(candidateMetadata.getMainMethod().getReturnDataEvent().getValue());
        this.methodArguments = TestCandidateUtils.buildArgumentValuesFromTestCandidate(candidateMetadata);
        this.returnValueClassname = candidateMetadata.getMainMethod().getReturnValue().getType();
        this.methodUnderTest = MethodUnderTest.fromTestCandidateMetadata(candidateMetadata);
        if (candidateMetadata.getMainMethod().getReturnValue().getProb() != null) {
            this.probSerializedValue = candidateMetadata.getMainMethod().getReturnValue().getProb()
                    .getSerializedValue();
        }
        this.sessionIdentifier = generateIdentifier(candidateMetadata);
        this.createdAt = candidateMetadata.getCreatedAt();
        this.entryProbeIndex = candidateMetadata.getEntryProbeIndex();
        this.lineNumbers = candidateMetadata.getLineNumbers();
        this.metadata = new StoredCandidateMetadata(
                HOSTNAME, HOSTNAME, candidateMetadata.getMainMethod().getEntryProbe().getRecordedAt()
        );
    }

    public static StoredCandidate createCandidateFor(StoredCandidate metadata) {
        StoredCandidate candidate = new StoredCandidate();
        candidate.setCandidateId(metadata.getCandidateId());
        candidate.setMethod(metadata.getMethod());
        candidate.setMethodArguments(metadata.getMethodArguments());
        candidate.setLineNumbers(metadata.getLineNumbers());
        candidate.setException(metadata.isException());
        candidate.setCreatedAt(metadata.getCreatedAt());
        candidate.setReturnValue(metadata.getReturnValue());
        candidate.setMockIds(metadata.getMockIds());
        //to be updated
        candidate.setProbSerializedValue(metadata.getProbSerializedValue());
        //to be updated
        candidate.setSessionIdentifier(metadata.getSessionIdentifier());
        candidate.setEntryProbeIndex(metadata.getEntryProbeIndex());
        candidate.setReturnValueClassname(metadata.getReturnValueClassname());

        if (metadata.getMetadata() != null) {
            candidate.setMetadata(metadata.getMetadata());
            candidate.getMetadata().setHostMachineName(HOSTNAME);
            candidate.getMetadata().setRecordedBy(HOSTNAME);
        } else {
            StoredCandidateMetadata metadata1 = new StoredCandidateMetadata(
                    HOSTNAME, HOSTNAME, metadata.getCreatedAt()
            );
            candidate.setMetadata(metadata1);
        }
        return candidate;
    }

    private long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Set<String> getMockIds() {
        return mockIds;
    }

    public void setMockIds(Set<String> enabledMockDefinition) {
        this.mockIds = enabledMockDefinition;
    }

    public long getEntryProbeIndex() {
        return entryProbeIndex;
    }

    public void setEntryProbeIndex(long entryProbeIndex) {
        this.entryProbeIndex = entryProbeIndex;
    }

    public List<Integer> getLineNumbers() {
        return lineNumbers;
    }

    public void setLineNumbers(List<Integer> lineNumbers) {
        this.lineNumbers = lineNumbers;
    }

    @Override
    public int hashCode() {
        return Objects.requireNonNullElseGet(this.candidateId,
                () -> this.sessionIdentifier + "-" + this.metadata.getHostMachineName()).hashCode();
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

        return this.sessionIdentifier == otherCandidate.getSessionIdentifier();
    }

    @Override
    public int compareTo(StoredCandidate o) {
        return this.candidateId.compareTo(o.candidateId);
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

    public long getSessionIdentifier() {
        return sessionIdentifier;
    }

    public void setSessionIdentifier(long sessionIdentifier) {
        this.sessionIdentifier = sessionIdentifier;
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
                ", entryProbeIndex=" + sessionIdentifier +
                '}';
    }

    private long generateIdentifier(TestCandidateMetadata candidateMetadata) {
        String id = "" + candidateMetadata.getMainMethod().getEntryProbe().getRecordedAt()
                + candidateMetadata.getEntryProbeIndex();
        return id.hashCode();
    }

    public String calculateCandidateHash() {
        return String.valueOf((methodArguments.toString() + "->" + returnValue).hashCode());
    }

    public void copyFrom(StoredCandidate candidate) {
        this.setCandidateId(candidate.getCandidateId());
        this.setName(candidate.getName());
        this.setDescription(candidate.getDescription());
        this.setReturnValue(candidate.getReturnValue());
        this.setSessionIdentifier(candidate.getSessionIdentifier());
        this.setEntryProbeIndex(candidate.getEntryProbeIndex());
        this.setProbSerializedValue(candidate.getProbSerializedValue());
        this.setException(candidate.isException());
        this.setReturnValueClassname(candidate.getReturnValueClassname());
        this.setCreatedAt(candidate.getCreatedAt());
        this.setLineNumbers(candidate.getLineNumbers());
        this.setTestAssertions(candidate.getTestAssertions());
        this.setMockIds(candidate.getMockIds());
    }

    public AtomicAssertion getTestAssertions() {
        return testAssertions;
    }

    public void setTestAssertions(AtomicAssertion testAssertions) {
        this.testAssertions = testAssertions;
    }
}
