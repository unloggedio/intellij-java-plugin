package com.insidious.plugin.pojo;

import com.insidious.common.weaver.ObjectInfo;
import com.insidious.plugin.extension.model.ReplayData;

public class ObjectHistory {
    private final ReplayData replayData;
    private final ObjectInfo objectInfo;


    public ReplayData getReplayData() {
        return replayData;
    }

    public ObjectInfo getObjectInfo() {
        return objectInfo;
    }

    public ObjectHistory(ReplayData replayData, ObjectInfo objectInfo) {
        this.replayData = replayData;
        this.objectInfo = objectInfo;
    }
}

