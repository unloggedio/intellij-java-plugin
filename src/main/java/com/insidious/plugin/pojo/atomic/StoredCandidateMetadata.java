package com.insidious.plugin.pojo.atomic;

public class StoredCandidateMetadata {
    private String recordedBy;
    private String hostMachineName;

    private long timestamp;
    private CandidateStatus candidateStatus;

    public String getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(String recordedBy) {
        this.recordedBy = recordedBy;
    }

    public String getHostMachineName() {
        return hostMachineName;
    }

    public void setHostMachineName(String hostMachineName) {
        this.hostMachineName = hostMachineName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public CandidateStatus getCandidateStatus() {
        return candidateStatus;
    }

    public void setCandidateStatus(CandidateStatus candidateStatus) {
        this.candidateStatus = candidateStatus;
    }

    @Override
    public String toString() {
        return "StoredCandidateMetadata{" +
                "recordedBy='" + recordedBy + '\'' +
                ", hostMachineName='" + hostMachineName + '\'' +
                ", timestamp=" + timestamp +
                ", candidateStatus=" + candidateStatus +
                '}';
    }

    public enum CandidateStatus {PASSING, FAILING}
}
