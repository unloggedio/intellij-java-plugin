package com.insidious.plugin.ui;

import com.insidious.common.weaver.ClassInfo;
import com.insidious.common.weaver.MethodInfo;
import com.insidious.plugin.extension.descriptor.renderer.InsidiousDebuggerTreeNodeImpl;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

public class TreeClassInfoModel {
    private final List<TreeModelListener> treeModelListeners = new LinkedList<>();
    private final String className;
    private final String sessionId;
    private final String simpleClassName;

    public TreeClassInfoModel(String className, String sessionId) {


        this.className = className;
        String[] classnameParts = className.split("/");
        this.simpleClassName = classnameParts[classnameParts.length - 1];
        this.sessionId = sessionId;
    }

    @Override
    public String toString() {
        return simpleClassName;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getClassName() {
        return className;
    }






}
