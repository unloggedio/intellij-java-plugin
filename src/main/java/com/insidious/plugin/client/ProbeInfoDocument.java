package com.insidious.plugin.client;

import com.googlecode.cqengine.attribute.SimpleAttribute;
import com.googlecode.cqengine.query.option.QueryOptions;
import com.insidious.common.cqengine.ObjectInfoDocument;
import com.insidious.common.weaver.Descriptor;
import com.insidious.common.weaver.EventType;

import java.io.Serializable;
import java.util.Map;

public class ProbeInfoDocument implements Serializable {


    public static final SimpleAttribute<ProbeInfoDocument, Integer> PROBE_ID =
            new SimpleAttribute<ProbeInfoDocument, Integer>("objectId") {
                public Integer getValue(ProbeInfoDocument probeInfoDocument, QueryOptions queryOptions) {
                    return probeInfoDocument.dataId;
                }
            };

    private int classId;
    private int methodId;
    private int dataId;
    private int index;
    private int line;
    private int instructionIndex;
    private EventType eventType;
    private Descriptor valueDesc;
    private String attributes;
    private Map<String, String> attributesMap;

    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) {
        this.classId = classId;
    }

    public int getMethodId() {
        return methodId;
    }

    public void setMethodId(int methodId) {
        this.methodId = methodId;
    }

    public int getDataId() {
        return dataId;
    }

    public void setDataId(int dataId) {
        this.dataId = dataId;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getInstructionIndex() {
        return instructionIndex;
    }

    public void setInstructionIndex(int instructionIndex) {
        this.instructionIndex = instructionIndex;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public Descriptor getValueDesc() {
        return valueDesc;
    }

    public void setValueDesc(Descriptor valueDesc) {
        this.valueDesc = valueDesc;
    }

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public Map<String, String> getAttributesMap() {
        return attributesMap;
    }

    public void setAttributesMap(Map<String, String> attributesMap) {
        this.attributesMap = attributesMap;
    }
}
