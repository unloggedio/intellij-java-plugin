package com.insidious.plugin.ui;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.*;

public class PackageInfoModel {

    public Vector<TreeClassInfoModel> getChildren() {
        return children;
    }

    private final Vector<TreeClassInfoModel> children;
    String packageNamePart;
    private String sessionId;

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

    public PackageInfoModel(String packageNamePart, String sessionId) {
        this.packageNamePart = packageNamePart;
        this.sessionId = sessionId;
        this.children = new Vector<>();
    }

    @Override
    public String toString() {
        return packageNamePart;
    }

    public void addClassInfo(TreeClassInfoModel treeInfoModel) {
        this.children.add(treeInfoModel);
    }
}
