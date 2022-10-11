package com.insidious.plugin.client.pojo;


import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

/**
 * This object is to record attributes of a data ID but also serializes the session id in response
 */

@DatabaseTable(tableName = "data_event")
public class DataEventWithSessionId implements Serializable {

    @DatabaseField
    private long threadId;
    @DatabaseField(id = true)
    private long nanoTime;
    @DatabaseField
    private long recordedAt;

    @DatabaseField
    private long dataId;

    @DatabaseField
    private long value;
    private String sessionId;
    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    private byte[] serializedValue = new byte[0];

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

    public long getDataId() {
        return dataId;
    }

    public void setDataId(long dataId) {
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

    public DataEventWithSessionId(long threadId) {
        this.threadId = threadId;
    }

    public DataEventWithSessionId() {
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
