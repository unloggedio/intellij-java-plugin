package com.insidious.plugin.client.pojo;


import java.util.Date;

/**
 * This object is to record attributes of a data ID but also serializes the session id in response
 */
public class DataEventWithSessionId {

    private long threadId;
    private long nanoTime;
    private long recordedAt;

    private int dataId;

    private long value;
    private String sessionId;
    private byte[] serializedValue;

    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    public long getNanoTime() {
        return nanoTime;
    }

    public void setNanoTime(long nanoTime) {
        this.nanoTime = nanoTime;
    }

    public long getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(long recordedAt) {
        this.recordedAt = recordedAt;
    }

    public int getDataId() {
        return dataId;
    }

    public void setDataId(int dataId) {
        this.dataId = dataId;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String toString() {
        return "DataEventWithSessionId{" +
                "threadId=" + threadId +
                ", nanoTime=" + nanoTime +
                ", recordedAt=" + recordedAt +
                ", dataId=" + dataId +
                ", value=" + value +
                ", sessionId='" + sessionId + '\'' +
                '}';
    }

    public void setSerializedValue(byte[] serializedValue) {
        this.serializedValue = serializedValue;
    }

    public byte[] getSerializedValue() {
        return serializedValue;
    }
}
