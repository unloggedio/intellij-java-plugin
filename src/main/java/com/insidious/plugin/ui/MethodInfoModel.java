package com.insidious.plugin.ui;

import javax.swing.tree.DefaultMutableTreeNode;

public class MethodInfoModel extends DefaultMutableTreeNode {
    private final String methodName;
    private final String className;
    private final String sessionId;

    public String getMethodName() {
        return methodName;
    }

    public String getClassName() {
        return className;
    }

    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String toString() {
        return methodName;
    }

    public MethodInfoModel(String methodName, String className, String sessionId) {
        this.methodName = methodName;
        this.className = className;
        this.sessionId = sessionId;
    }
}
