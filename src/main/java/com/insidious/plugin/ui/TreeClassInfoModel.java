package com.insidious.plugin.ui;

import javax.swing.event.TreeModelListener;
import java.util.LinkedList;
import java.util.List;

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
