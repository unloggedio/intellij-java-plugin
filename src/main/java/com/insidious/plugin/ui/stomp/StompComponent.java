package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.factory.InsidiousService;
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import com.intellij.util.ui.ColumnInfo;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

public class StompComponent {
    private JPanel mainPanel;

    public StompComponent(InsidiousService insidiousService) {
        TreeNode treeRootNode = new DefaultMutableTreeNode("Hello Tree Table");

        ColumnInfo[] columnInfo = new ColumnInfo[]{
                new ColumnInfo("Column 1") {
                    @Nullable
                    @Override
                    public Object valueOf(Object o) {
                        return "Col 1 value: " + o;
                    }
                }
        };
        TreeTableModel treeTableModel = new ListTreeTableModelOnColumns(treeRootNode, columnInfo);
        new TreeTable(treeTableModel);
    }

    public JComponent getComponent() {
        return mainPanel;
    }
}
