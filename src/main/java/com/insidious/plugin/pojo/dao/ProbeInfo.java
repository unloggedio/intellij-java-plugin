package com.insidious.plugin.pojo.dao;

import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.Descriptor;
import com.insidious.common.weaver.EventType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "probe_info")
public class ProbeInfo {

    private static String SEPARATOR = ",";
    private static char ATTRIBUTE_KEYVALUE_SEPARATOR = '=';
    private static char ATTRIBUTE_SEPARATOR = ',';

    @DatabaseField
    private int classId;
    @DatabaseField
    private int methodId;
    @DatabaseField(id = true)
    private int dataId;
    @DatabaseField
    private int line;
    @DatabaseField
    private int instructionIndex;
    @DatabaseField
    private EventType eventType;
    @DatabaseField
    private Descriptor valueDesc;
    @DatabaseField
    private String attributes;

    public static DataInfo ToProbeInfo(ProbeInfo dataInfo) {
        return new DataInfo(
                dataInfo.getClassId(), dataInfo.getMethodId(), dataInfo.getDataId(),
                dataInfo.getLine(), dataInfo.getInstructionIndex(), dataInfo.getEventType(),
                dataInfo.getValueDesc(), dataInfo.getAttributes()
        );
//        return new DataInfo();
    }

    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) {
        this.classId = classId;
    }

    public void setMethodId(int methodId) {
        this.methodId = methodId;
    }

    public void setDataId(int dataId) {
        this.dataId = dataId;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public void setInstructionIndex(int instructionIndex) {
        this.instructionIndex = instructionIndex;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public void setValueDesc(Descriptor valueDesc) {
        this.valueDesc = valueDesc;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public int getMethodId() {
        return methodId;
    }

    public int getDataId() {
        return dataId;
    }

    public int getLine() {
        return line;
    }

    public int getInstructionIndex() {
        return instructionIndex;
    }

    public EventType getEventType() {
        return eventType;
    }

    public Descriptor getValueDesc() {
        return valueDesc;
    }

    public String getAttributes() {
        return attributes;
    }

    public ProbeInfo() {
    }

    public ProbeInfo(int classId, int methodId, int dataId, int line,
                     int instructionIndex, EventType eventType,
                     Descriptor valueDesc, String attributes) {
        this.classId = classId;
        this.methodId = methodId;
        this.dataId = dataId;
        this.line = line;
        this.instructionIndex = instructionIndex;
        this.eventType = eventType;
        this.valueDesc = valueDesc;
        this.attributes = attributes;
    }

    public static ProbeInfo FromProbeInfo(DataInfo dataInfo) {
        if (dataInfo == null){
            return null;
        }
        return new ProbeInfo(
                dataInfo.getClassId(), dataInfo.getMethodId(), dataInfo.getDataId(),
                dataInfo.getLine(), dataInfo.getInstructionIndex(), dataInfo.getEventType(),
                dataInfo.getValueDesc(), dataInfo.getAttributes()
        );
    }

    public String getAttribute(String key, String defaultValue) {
        int index = attributes.indexOf(key);
        while (index >= 0) {
            if (index == 0 || attributes.charAt(index - 1) == ATTRIBUTE_SEPARATOR) {
                int keyEndIndex = attributes.indexOf(ATTRIBUTE_KEYVALUE_SEPARATOR, index);
                if (keyEndIndex == index + key.length()) {
                    int valueEndIndex = attributes.indexOf(ATTRIBUTE_SEPARATOR, keyEndIndex);
                    if (valueEndIndex > keyEndIndex) {
                        return attributes.substring(index + key.length() + 1, valueEndIndex);
                    } else {
                        return attributes.substring(index + key.length() + 1);
                    }
                }
            }
            index = attributes.indexOf(key, index + 1);
        }
        return defaultValue;
    }
}
