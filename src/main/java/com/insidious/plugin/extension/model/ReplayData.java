package com.insidious.plugin.extension.model;

import com.insidious.common.weaver.*;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;

import java.util.List;
import java.util.Map;

public class ReplayData {
    private final String sortOrder;
    private final VideobugClientInterface client;
    List<DataEventWithSessionId> dataEvents;
    Map<String, ClassInfo> classInfoMap;
    Map<String, DataInfo> dataInfoMap;
    Map<String, StringInfo> stringInfoMap;
    Map<String, ObjectInfo> objectInfo;
    Map<String, TypeInfo> typeInfo;

    public ReplayData(VideobugClientInterface client,
                      List<DataEventWithSessionId> dataList,
                      Map<String, ClassInfo> classInfo,
                      Map<String, DataInfo> dataInfo,
                      Map<String, StringInfo> stringInfo,
                      Map<String, ObjectInfo> objectInfo,
                      Map<String, TypeInfo> typeInfo,
                      String sortOrder) {
        this.client = client;
        dataEvents = dataList;
        classInfoMap = classInfo;
        dataInfoMap = dataInfo;
        stringInfoMap = stringInfo;
        this.objectInfo = objectInfo;
        this.typeInfo = typeInfo;
        this.sortOrder = sortOrder;
    }

    public Map<String, ObjectInfo> getObjectInfo() {
        return objectInfo;
    }

    public Map<String, TypeInfo> getTypeInfo() {
        return typeInfo;
    }

    public List<DataEventWithSessionId> getDataEvents() {
        return dataEvents;
    }


    public List<DataEventWithSessionId> getDataEvents(PageInfo pageInfo) {
        return dataEvents;
    }


    public Map<String, ClassInfo> getClassInfoMap() {
        return classInfoMap;
    }

    public Map<String, DataInfo> getDataInfoMap() {
        return dataInfoMap;
    }

    public Map<String, StringInfo> getStringInfoMap() {
        return stringInfoMap;
    }
}
