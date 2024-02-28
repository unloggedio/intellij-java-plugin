package com.insidious.plugin.ui.methodscope;

import com.intellij.ui.KeyStrokeAdapter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.treeStructure.Tree;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.EventObject;

public class InsidiousCellEditor extends DefaultTreeCellEditor {
    private final Tree argumentValueTree;
    private JTextField editor;
    private String key;
    Boolean JSONnode = false;

    public InsidiousCellEditor(Tree argumentValueTree, DefaultTreeCellRenderer renderer) {
        super(argumentValueTree, renderer);
        this.argumentValueTree = argumentValueTree;
    }

    @Override
    public boolean isCellEditable(EventObject event) {
        boolean isEditable = super.isCellEditable(event);
        if (isEditable) {
            // Make sure only leaf nodes are editable
            TreePath path = tree.getSelectionPath();
            return path != null && tree.getModel().isLeaf(path.getLastPathComponent());
        }
        return false;
    }

    @Override
    public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {

        editor = new JBTextField();
        if (value instanceof DefaultMutableTreeNode) {
            Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            if (userObject.toString().matches("\\{.*\\}")) {
                // this is a JSON node
                JSONnode = true;
                editor.setText(userObject.toString());
            }
            else {
                // this is a normal data
                String[] parts = userObject.toString().split(":");
                key = parts[0];
                editor.setText(parts.length < 2 ? "" : parts[1].trim());
            }

            editor.addKeyListener(new KeyStrokeAdapter() {
                @Override
                public void keyTyped(KeyEvent event) {
                    super.keyTyped(event);
                    if (event.getKeyChar() == KeyEvent.VK_ENTER) {
                        InsidiousCellEditor.this.stopCellEditing();
                    }
                }
            });
        }
        editor.setBorder(null);
		editor.setMinimumSize(new Dimension(100, 40));

        return editor;
    }

    @Override
    public boolean stopCellEditing() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) argumentValueTree.getLastSelectedPathComponent();
        if (node != null && node.isLeaf()) {
            DefaultTreeModel model = (DefaultTreeModel) argumentValueTree.getModel();
            node.setUserObject(getCellEditorValue());
            // model.nodeChanged(node); // Notify the model that the node has changed
        }
        return super.stopCellEditing();
    }

    @Override
    public Object getCellEditorValue() {
        if (JSONnode) {
            return editor.getText().trim();
        }
        else {
            return key + ": " + editor.getText().trim();
        }
    }

}
