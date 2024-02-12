package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class InsidiousTreeListener implements TreeModelListener {
    private static final Logger logger = LoggerUtil.getInstance(InsidiousTreeListener.class);

    public InsidiousTreeListener() {
        super();
    }

    @Override
    public void treeNodesChanged(TreeModelEvent e) {
        TreePath path = e.getTreePath();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        try {
            String editedValue = (String) node.getUserObject();
            // Here, you can further process the edited value if necessary
            // For example, validate or parse it before saving
            node.setUserObject(editedValue);
        } catch (Exception ex) {
            logger.error("Failed to read value", ex);
            InsidiousNotification.notifyMessage("Failed to read value, please report this on github",
                    NotificationType.ERROR);
        }
    }

    public void treeNodesInserted(TreeModelEvent e) {
    }

    public void treeNodesRemoved(TreeModelEvent e) {
    }

    public void treeStructureChanged(TreeModelEvent e) {
    }

}
