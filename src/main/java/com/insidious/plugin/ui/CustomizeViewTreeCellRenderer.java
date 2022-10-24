package com.insidious.plugin.ui;

import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.util.Vector;

public class CustomizeViewTreeCellRenderer implements TreeCellRenderer {
    private JCheckBox checkboxRenderer = new JCheckBox();
    private DefaultTreeCellRenderer rootRenderer = new DefaultTreeCellRenderer();

    Color selectionBorderColor, selectionForeground, selectionBackground,
            textForeground, textBackground;

    protected JCheckBox getLeafRenderer() {
        return checkboxRenderer;
    }

    public CustomizeViewTreeCellRenderer() {
        Font fontValue;
        fontValue = UIManager.getFont("Tree.font");
        if (fontValue != null) {
            checkboxRenderer.setFont(fontValue);
        }
        Boolean booleanValue = (Boolean) UIManager
                .get("Tree.drawsFocusBorderAroundIcon");
        checkboxRenderer.setFocusPainted((booleanValue != null)
                && (booleanValue.booleanValue()));

        selectionBorderColor = UIManager.getColor("Tree.selectionBorderColor");
        selectionForeground = UIManager.getColor("Tree.selectionForeground");
        selectionBackground = UIManager.getColor("Tree.selectionBackground");
        textForeground = UIManager.getColor("Tree.textForeground");
        textBackground = UIManager.getColor("Tree.textBackground");
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean selected, boolean expanded, boolean leaf, int row,
                                                  boolean hasFocus) {

        Component returnValue;
        //if (leaf) {

            String stringValue = tree.convertValueToText(value, selected,
                    expanded, leaf, row, false);
        checkboxRenderer.setText(stringValue);
        checkboxRenderer.setSelected(false);

        checkboxRenderer.setEnabled(tree.isEnabled());

        checkboxRenderer.setSelected(selected);
            if (selected) {
                checkboxRenderer.setForeground(selectionForeground);
                checkboxRenderer.setBackground(selectionBackground);
            } else {
                checkboxRenderer.setForeground(textForeground);
                checkboxRenderer.setBackground(textBackground);
            }

            if(leaf)
            {
                if ((value != null) && (value instanceof DefaultMutableTreeNode)) {
                    Object userObject = ((DefaultMutableTreeNode) value)
                            .getUserObject();
                    if (userObject instanceof CheckBoxNode) {
                        CheckBoxNode node = (CheckBoxNode) userObject;
                        checkboxRenderer.setText(node.getText());
                        checkboxRenderer.setSelected(node.isSelected());
                    }
                }
                returnValue = checkboxRenderer;
            }
            else
            {
                if(value instanceof TestCandidateTreeModel.RootNode == false) //not root
                {
                    if ((value != null) && (value instanceof DefaultMutableTreeNode)) {
                        Object userObject = ((DefaultMutableTreeNode) value)
                                .getUserObject();
                        if (userObject instanceof CheckBoxNode) {
                            CheckBoxNode node = (CheckBoxNode) userObject;
                            checkboxRenderer.setText(node.getText());
                            checkboxRenderer.setSelected(node.isSelected());
                        }
                    }
                    returnValue = checkboxRenderer;
                }
                else
                {
                    returnValue = rootRenderer.getTreeCellRendererComponent(tree,
                            value, selected, expanded, leaf, row, hasFocus);
                }
            }
        return returnValue;
    }
}

class CheckBoxNode {
    String text;
    boolean selected;

    public CheckBoxNode(String text, boolean selected) {
        this.text = text;
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean newValue) {
        selected = newValue;
    }

    public String getText() {
        return text;
    }

    public void setText(String newValue) {
        text = newValue;
    }

    public String toString() {
        return getClass().getName() + "[" + text + "/" + selected + "]";
    }
}


