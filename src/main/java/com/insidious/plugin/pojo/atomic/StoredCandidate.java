package com.insidious.plugin.pojo.atomic;

import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.util.TestCandidateUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class StoredCandidate implements Comparable {

    private String candidateId;
    private String name;
    private String description;
    private List<String> methodArguments;
    private String returnValue;
    private boolean isException;
    private String returnValueClassname;
    private StoredCandidateMetadata metadata;
    private String methodHash;
    private boolean returnValueIsBoolean;
    private AssertionType assertionType;
    private long entryProbeIndex;
    private String returnDataEventSerializedValue;
    private long returnDataEventValue;
    private String methodName;
    private byte[] probSerializedValue;
    private String methodSignature;
    private String className;

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
        this.setReturnValueIsBoolean(candidateMetadata.getMainMethod().getReturnValue().isBooleanType());
        this.setMethodName(candidateMetadata.getMainMethod().getMethodName());
        this.setProbSerializedValue(candidateMetadata.getMainMethod().getReturnValue().getProb().getSerializedValue());
        this.setEntryProbeIndex(generateIdentifier(candidateMetadata));
        StoredCandidateMetadata metadata = new StoredCandidateMetadata();
        metadata.setTimestamp(candidateMetadata.getCallTimeNanoSecond());
        this.setMetadata(metadata);
    }

    @Override
    public int hashCode() {
        if (this.candidateId != null) {
            return this.candidateId.hashCode();
        }
        return (this.entryProbeIndex + "-" + this.metadata.getHostMachineName()).hashCode();
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
    public int compareTo(@NotNull Object o) {
        if (!(o instanceof StoredCandidate)) {
            return -1;
        }
        return Long.compare(this.entryProbeIndex, ((StoredCandidate) o).entryProbeIndex);
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

    public String getMethodHash() {
        return methodHash;
    }

    public void setMethodHash(String methodHash) {
        this.methodHash = methodHash;
    }

    public AssertionType getAssertionType() {
        return assertionType;
    }

    public void setAssertionType(AssertionType assertionType) {
        this.assertionType = assertionType;
    }

    public long getEntryProbeIndex() {
        return entryProbeIndex;
    }

    public void setEntryProbeIndex(long entryProbeIndex) {
        this.entryProbeIndex = entryProbeIndex;
    }

    public boolean isReturnValueIsBoolean() {
        return returnValueIsBoolean;
    }

    public void setReturnValueIsBoolean(boolean returnValueIsBoolean) {
        this.returnValueIsBoolean = returnValueIsBoolean;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
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
                ", methodHash='" + methodHash + '\'' +
                ", BooleanType=" + returnValueIsBoolean +
                ", assertionType=" + assertionType +
                ", entryProbeIndex=" + entryProbeIndex +
                ", returnDataEventSerializedValue=" + returnDataEventSerializedValue +
                ", returnDataEventValue=" + returnDataEventValue +
                ", methodName='" + methodName + '\'' +
                ", probSerializedValue=" + Arrays.toString(probSerializedValue) +
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
        this.setAssertionType(candidate.getAssertionType());
        this.setReturnValue(candidate.getReturnValue());
        this.setEntryProbeIndex(candidate.getEntryProbeIndex());
        this.setReturnValueIsBoolean(candidate.isReturnValueIsBoolean());
        this.setProbSerializedValue(candidate.getProbSerializedValue());
        this.setException(candidate.isException());
        this.setReturnValueClassname(candidate.getReturnValueClassname());
    }

    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public enum AssertionType {EQUAL, NOT_EQUAL}
}
