package com.insidious.plugin.client.pojo;


import com.j256.ormlite.table.DatabaseTable;

/**
 * This object is to record attributes of a data ID but also serializes the session id in response
 */

@DatabaseTable(tableName = "data_event")
public interface DataEventInterface {
    
    long getThreadId();

    void setThreadId(long threadId);

    long getNanoTime();

    void setNanoTime(long nanoTime);

    long getRecordedAt();

    void setRecordedAt(long recordedAt);

    long getDataId();

    void setDataId(long dataId);

    long getValue();

    void setValue(long value);

    String getSessionId();

    void setSessionId(String sessionId);

    byte[] getSerializedValue();

    void setSerializedValue(byte[] serializedValue);
}
