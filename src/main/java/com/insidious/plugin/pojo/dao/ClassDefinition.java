package com.insidious.plugin.pojo.dao;

import com.insidious.common.weaver.ClassInfo;
import com.insidious.plugin.util.StringUtils;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Arrays;
import java.util.Objects;

@DatabaseTable(tableName = "class_definition")
public class ClassDefinition {

    @DatabaseField(id = true)
    private long id;

    @DatabaseField
    private String className;
    @DatabaseField
    private boolean isStatic;
    private boolean isPojo;
    private boolean isEnum;

    @DatabaseField
    private String fields;

    @DatabaseField
    private String interfaceList;

    @DatabaseField
    private String superName;
    @DatabaseField
    private String signature;

    @DatabaseField
    private String container;
    @DatabaseField
    private String filename;
    @DatabaseField
    private String loglevel;
    @DatabaseField
    private String hash;
    @DatabaseField
    private String classLoaderIdentifier;


    public ClassDefinition() {
    }

    public static ClassDefinition fromClassInfo(ClassInfo e) {

        ClassDefinition classDefinition = new ClassDefinition();
        classDefinition.setClassName(e.getClassName());
        classDefinition.setClassLoaderIdentifier(e.getClassLoaderIdentifier());

        classDefinition.setId(e.getClassId());
        classDefinition.setEnum(e.isEnum());
        classDefinition.setPojo(e.isPojo());
        classDefinition.setFilename(e.getFilename());
        classDefinition.setHash(e.getHash());
        classDefinition.setContainer(e.getContainer());
        classDefinition.setLoglevel(e.getLoglevel()
                .toString());
        classDefinition.setInterfaceList(StringUtils.join(Arrays.asList(e.getInterfaces()), ","));
        classDefinition.setSuperName(e.getSuperName());
        classDefinition.setSignature(e.getClassName());

        return classDefinition;
    }


    public boolean isPojo() {
        return isPojo;
    }

    public void setPojo(boolean pojo) {
        isPojo = pojo;
    }

    public boolean isEnum() {
        return isEnum;
    }

    public void setEnum(boolean anEnum) {
        isEnum = anEnum;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }

    public String getFields() {
        return fields;
    }

    public void setFields(String fields) {
        this.fields = fields;
    }

    public String getInterfaceList() {
        return interfaceList;
    }

    public void setInterfaceList(String interfaceList) {
        this.interfaceList = interfaceList;
    }

    public String getSuperName() {
        return superName;
    }

    public void setSuperName(String superName) {
        this.superName = superName;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getLoglevel() {
        return loglevel;
    }

    public void setLoglevel(String loglevel) {
        this.loglevel = loglevel;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getClassLoaderIdentifier() {
        return classLoaderIdentifier;
    }

    public void setClassLoaderIdentifier(String classLoaderIdentifier) {
        this.classLoaderIdentifier = classLoaderIdentifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassDefinition that = (ClassDefinition) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
