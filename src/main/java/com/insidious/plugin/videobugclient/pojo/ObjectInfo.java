package com.insidious.plugin.videobugclient.pojo;


import java.util.Date;

public class ObjectInfo {


    String sessionId;

    private Date recordedAt;

    private long typeId;
    private long objectId;


    public ObjectInfo() {
    }

    public ObjectInfo(long objectId, long typeId, Date recordedAt) {

        this.objectId = objectId;
        this.typeId = typeId;
        this.recordedAt = recordedAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Date getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Date recordedAt) {
        this.recordedAt = recordedAt;
    }

    public long getTypeId() {
        return typeId;
    }

    public void setTypeId(long typeId) {
        this.typeId = typeId;
    }

    public long getObjectId() {
        return objectId;
    }

    public void setObjectId(long objectId) {
        this.objectId = objectId;
    }
}
