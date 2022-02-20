package extension.model;

import network.pojo.ClassInfo;
import network.pojo.DataEventWithSessionId;

import java.util.List;
import java.util.Map;

public class ReplayData {
    List<DataEventWithSessionId> dataEvents;
    Map<String, ClassInfo> classInfoMap;
    Map<String, DataInfo> dataInfoMap;
    Map<String, StringInfo> stringInfoMap;

    public ReplayData(List<DataEventWithSessionId> dataList, Map<String, ClassInfo> classInfo, Map<String, DataInfo> dataInfo, Map<String, StringInfo> stringInfo) {
        dataEvents = dataList;
        classInfoMap = classInfo;
        dataInfoMap = dataInfo;
        stringInfoMap = stringInfo;
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
