package com.insidious.plugin.client.pojo.local;

import com.googlecode.cqengine.attribute.SimpleAttribute;
import com.googlecode.cqengine.query.option.QueryOptions;

import java.io.Serializable;

public class TypeInfoDocument implements Serializable {

    public static final SimpleAttribute<TypeInfoDocument, String> TYPE_NAME =
            new SimpleAttribute<TypeInfoDocument, String>("typeName") {
                public String getValue(TypeInfoDocument typeInfoDocument, QueryOptions queryOptions) {
                    return typeInfoDocument.typeName;
                }
            };
    private static final long serialVersionUID = 4357600885262072086L;


    private int typeId;
    private String typeName;

    @Override
    public String toString() {
        return "TypeInfoDocument{" +
                "typeId=" + typeId +
                ", typeName='" + typeName + '\'' +
                '}';
    }

    public TypeInfoDocument() {
    }

    public TypeInfoDocument(int typeId, String typeName) {
        this.typeId = typeId;
        this.typeName = typeName;
    }

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }
}
