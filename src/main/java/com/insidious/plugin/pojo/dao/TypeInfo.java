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
    long id;
    @DatabaseField
    private String interfaces;
    @DatabaseField
    private String name;
    @DatabaseField
    private String classLocation;
    @DatabaseField
    private long superClass;
    @DatabaseField
    private String container;
    @DatabaseField
    private String classLoaderIdentifier;
    private String fileName;
    private String signature;

    public TypeInfo() {
    }

    public TypeInfo(int id, String name,
                    String classLocation, int superClass,
                    String container, String classLoaderIdentifier, int[] interfaces) {
        this.id = id;
        this.name = name;
        this.classLocation = classLocation;
        this.superClass = superClass;
        this.container = container;
        this.classLoaderIdentifier = classLoaderIdentifier;
        this.interfaces = Strings.join(interfaces, ",");
    }

    public String getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(String interfaces) {
        this.interfaces = interfaces;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassLocation() {
        return classLocation;
    }

    public void setClassLocation(String classLocation) {
        this.classLocation = classLocation;
    }

    public long getSuperClass() {
        return superClass;
    }

    public void setSuperClass(long superClass) {
        this.superClass = superClass;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public String getClassLoaderIdentifier() {
        return classLoaderIdentifier;
    }

    public void setClassLoaderIdentifier(String classLoaderIdentifier) {
        this.classLoaderIdentifier = classLoaderIdentifier;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
