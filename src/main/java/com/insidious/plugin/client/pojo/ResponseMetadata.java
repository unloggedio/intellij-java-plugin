package com.insidious.plugin.client.pojo;

import com.insidious.common.parser.KaitaiInsidiousClassWeaveParser;
import com.insidious.common.weaver.*;

import java.util.HashMap;
import java.util.Map;

public class ResponseMetadata {
    Map<String, ClassInfo> classInfo;
    Map<String, DataInfo> dataInfo;
    Map<String, StringInfo> stringInfo;
    Map<String, TypeInfo> typeInfo;
    Map<String, ObjectInfo> objectInfo;

    public ResponseMetadata() {
        this.classInfo = new HashMap<String, ClassInfo>();
        this.dataInfo = new HashMap<>();
        this.stringInfo = new HashMap<>();
        this.typeInfo = new HashMap<>();
        this.objectInfo = new HashMap<>();
    }

    public Map<String, ObjectInfo> getObjectInfo() {
        return objectInfo;
    }

    public void setObjectInfo(Map<String, ObjectInfo> objectInfo) {
        this.objectInfo = objectInfo;
    }

    public Map<String, TypeInfo> getTypeInfo() {
        return typeInfo;
    }

    public void setTypeInfo(Map<String, TypeInfo> typeInfo) {
        this.typeInfo = typeInfo;
    }

    public Map<String, ClassInfo> getClassInfo() {
        return classInfo;
    }

    public void setClassInfo(Map<String, ClassInfo> classInfo) {
        this.classInfo = classInfo;
    }

    public Map<String, DataInfo> getDataInfo() {
        return dataInfo;
    }

    public void setDataInfo(Map<String, DataInfo> dataInfo) {
        this.dataInfo = dataInfo;
    }

    public Map<String, StringInfo> getStringInfo() {
        return stringInfo;
    }

    public void setStringInfo(Map<String, StringInfo> stringInfo) {
        this.stringInfo = stringInfo;
    }
}
