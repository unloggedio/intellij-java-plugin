package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

public abstract class InsidiousTreeListener implements TreeModelListener {
    private static final Logger logger = LoggerUtil.getInstance(InsidiousTreeListener.class);

    public InsidiousTreeListener() {
        super();
    }

    public void treeNodesInserted(TreeModelEvent e) {
    }

    public void treeNodesRemoved(TreeModelEvent e) {
    }

    public void treeStructureChanged(TreeModelEvent e) {
    }

}
