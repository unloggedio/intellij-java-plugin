package com.insidious.plugin.pojo.atomic;

import java.util.Arrays;
import java.util.List;

public class StoredCandidate {

    private String candidateId;
    private String name;
    private String description;
    private List<String> methodArguments;
    private String returnValue;
    private boolean isException;
    private String returnValueClassname;
    private StoredCandidateMetadata metadata;
    private String methodHash;
    private boolean BooleanType;
    public enum AssertionType {EQUAL, NOT_EQUAL}
    private AssertionType assertionType;
    private long entryProbeIndex;
    private String returnDataEventSerializedValue;
    private long returnDataEventValue;
    private String methodName;
    private byte[] probSerializedValue;
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

    public boolean isBooleanType() {
        return BooleanType;
    }

    public void setBooleanType(boolean booleanType) {
        BooleanType = booleanType;
    }

    public String getReturnDataEventSerializedValue() {
        return returnDataEventSerializedValue;
    }

    public void setReturnDataEventSerializedValue(String returnDataEventSerializedValue) {
        this.returnDataEventSerializedValue = returnDataEventSerializedValue;
    }

    public long getReturnDataEventValue() {
        return returnDataEventValue;
    }

    public void setReturnDataEventValue(long returnDataEventValue) {
        this.returnDataEventValue = returnDataEventValue;
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
                ", BooleanType=" + BooleanType +
                ", assertionType=" + assertionType +
                ", entryProbeIndex=" + entryProbeIndex +
                ", returnDataEventSerializedValue=" + returnDataEventSerializedValue +
                ", returnDataEventValue=" + returnDataEventValue +
                ", methodName='" + methodName + '\'' +
                ", probSerializedValue=" + Arrays.toString(probSerializedValue) +
                '}';
    }
}
