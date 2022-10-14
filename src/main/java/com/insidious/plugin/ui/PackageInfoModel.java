package com.insidious.plugin.ui;

import java.util.Vector;

public class PackageInfoModel {

    private final Vector<TreeClassInfoModel> children;
    String packageNamePart;
    private String sessionId;
    public PackageInfoModel(String packageNamePart, String sessionId) {
        this.packageNamePart = packageNamePart;
        this.sessionId = sessionId;
        this.children = new Vector<>();
    }

    public Vector<TreeClassInfoModel> getChildren() {
        return children;
    }

    public String getPackageNamePart() {
        return packageNamePart;
    }

    public void setPackageNamePart(String packageNamePart) {
        this.packageNamePart = packageNamePart;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String toString() {
        return packageNamePart;
    }

    public void addClassInfo(TreeClassInfoModel treeInfoModel) {
        this.children.add(treeInfoModel);
    }
}
