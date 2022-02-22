package extension.model;

import network.pojo.ClassInfo;
import network.pojo.DataEventWithSessionId;
import network.pojo.ObjectInfo;

import java.util.List;
import java.util.Map;

public class ReplayData {
    List<DataEventWithSessionId> dataEvents;
    Map<String, ClassInfo> classInfoMap;
    Map<String, DataInfo> dataInfoMap;
    Map<String, StringInfo> stringInfoMap;
    Map<String, ObjectInfo> objectInfo;
    Map<String, TypeInfo> typeInfo;

    public ReplayData(List<DataEventWithSessionId> dataList,
                      Map<String, ClassInfo> classInfo,
                      Map<String, DataInfo> dataInfo,
                      Map<String, StringInfo> stringInfo,
                      Map<String, ObjectInfo> objectInfo,
                      Map<String, TypeInfo> typeInfo
    ) {
        dataEvents = dataList;
        classInfoMap = classInfo;
        dataInfoMap = dataInfo;
        stringInfoMap = stringInfo;
        this.objectInfo = objectInfo;
        this.typeInfo = typeInfo;
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
