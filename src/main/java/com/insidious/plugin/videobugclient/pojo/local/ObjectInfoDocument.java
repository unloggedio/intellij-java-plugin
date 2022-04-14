package com.insidious.plugin.videobugclient.pojo.local;

import com.googlecode.cqengine.attribute.SimpleAttribute;
import com.googlecode.cqengine.query.option.QueryOptions;

public class ObjectInfoDocument {

    public static final SimpleAttribute<ObjectInfoDocument, Integer> OBJECT_TYPE_ID =
            new SimpleAttribute<ObjectInfoDocument, Integer>("objectTypeId") {
                public Integer getValue(ObjectInfoDocument typeInfoDocument, QueryOptions queryOptions) {
                    return typeInfoDocument.typeId;
                }
            };


    Long objectId;
    Integer typeId;

    public ObjectInfoDocument(long objectId, int typeId) {
        this.objectId = objectId;
        this.typeId = typeId;
    }

    public Long getObjectId() {
        return objectId;
    }

    public void setObjectId(Long objectId) {
        this.objectId = objectId;
    }

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }
}
