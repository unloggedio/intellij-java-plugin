package com.insidious.plugin.pojo.dao;


import com.intellij.openapi.util.text.Strings;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

@DatabaseTable(tableName = "type_info")
public class TypeInfo {

    @DatabaseField(id = true)
    String id;
    String sessionId;
    @DatabaseField
    private String interfaces;
    @DatabaseField
    private long typeId;
    @DatabaseField
    private String typeNameFromClass;
    @DatabaseField
    private String classLocation;
    @DatabaseField
    private int superClass;
    @DatabaseField
    private int componentType;
    @DatabaseField
    private String classLoaderIdentifier;

    public TypeInfo() {
    }

    public TypeInfo(String sessionId, long typeId, String typeNameFromClass,
                    String classLocation, int superClass,
                    int componentType, String classLoaderIdentifier, int[] interfaces) {
        this.sessionId = sessionId;
        this.typeId = typeId;
        this.typeNameFromClass = typeNameFromClass;
        this.classLocation = classLocation;
        this.superClass = superClass;
        this.componentType = componentType;
        this.classLoaderIdentifier = classLoaderIdentifier;
        this.interfaces = Strings.join(interfaces, ",");
    }

    public static TypeInfo fromBytes(byte[] typeBytes) {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(typeBytes));
        try {
            int typeId = dis.readInt();
            int nameLength = dis.readInt();

            byte[] typeNameBytes = new byte[nameLength];
            int readLength = dis.read(typeNameBytes);
            assert readLength == nameLength;
            String typeName = new String(typeNameBytes);

            int classLocationLength = dis.readInt();
            byte[] classLocationBytes = new byte[classLocationLength];
            readLength = dis.read(classLocationBytes);
            assert readLength == classLocationLength;

            String classLocation = new String(classLocationBytes);

            int superClass = dis.readInt();
            int componentClass = dis.readInt();

            int classLoaderIdentifierLength = dis.readInt();
            byte[] classLoaderIdentifierBytes = new byte[classLoaderIdentifierLength];
            readLength = dis.read(classLoaderIdentifierBytes);
            assert readLength == classLoaderIdentifierLength;
            String classLoaderIdentifier = new String(classLoaderIdentifierBytes);


            int interfaceCount = dis.readInt();
            int[] interfaces = new int[interfaceCount];
            for (int i = 0; i < interfaceCount; i++) {
                int interfaceId = dis.readInt();
                interfaces[i] = interfaceId;
            }

            return new TypeInfo("", typeId, typeName,
                    classLocation, superClass, componentClass, classLoaderIdentifier, interfaces);
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    public String getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(String interfaces) {
        this.interfaces = interfaces;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public long getTypeId() {
        return typeId;
    }

    public void setTypeId(long typeId) {
        this.typeId = typeId;
    }

    public String getTypeNameFromClass() {
        return typeNameFromClass;
    }

    public void setTypeNameFromClass(String typeNameFromClass) {
        this.typeNameFromClass = typeNameFromClass;
    }

    public String getClassLocation() {
        return classLocation;
    }

    public void setClassLocation(String classLocation) {
        this.classLocation = classLocation;
    }

    public int getSuperClass() {
        return superClass;
    }

    public void setSuperClass(int superClass) {
        this.superClass = superClass;
    }

    public int getComponentType() {
        return componentType;
    }

    public void setComponentType(int componentType) {
        this.componentType = componentType;
    }

    public String getClassLoaderIdentifier() {
        return classLoaderIdentifier;
    }

    public void setClassLoaderIdentifier(String classLoaderIdentifier) {
        this.classLoaderIdentifier = classLoaderIdentifier;
    }
}
