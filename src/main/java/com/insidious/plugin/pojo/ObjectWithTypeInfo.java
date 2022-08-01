package com.insidious.plugin.pojo;

import com.insidious.common.weaver.ObjectInfo;
import com.insidious.common.weaver.TypeInfo;

public class ObjectWithTypeInfo {
    private final ObjectInfo objectInfo;
    private final TypeInfo typeInfo;

    public ObjectWithTypeInfo(ObjectInfo objectInfo, TypeInfo typeInfo) {
        this.objectInfo = objectInfo;
        this.typeInfo = typeInfo;
    }

    public ObjectInfo getObjectInfo() {
        return objectInfo;
    }

    public TypeInfo getTypeInfo() {
        return typeInfo;
    }
}
