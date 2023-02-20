package com.insidious.plugin.ui;

import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.intellij.openapi.util.IconLoader;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public class CustomizeViewTreeCellRenderer implements TreeCellRenderer {
    Icon icon = IconLoader.getIcon("icons/png/method_v1.png", CustomizeViewTreeCellRenderer.class);
    Color selectionBorderColor;
    Color selectionForeground;
    Color selectionBackground;
    Color textForeground;
    Color textBackground;
    private final JCheckBox checkboxRenderer = new JCheckBox();
    private final DefaultTreeCellRenderer rootRenderer = new DefaultTreeCellRenderer();

    public CustomizeViewTreeCellRenderer() {
        Font fontValue;
        fontValue = UIManager.getFont("Tree.font");
        if (fontValue != null) {
            checkboxRenderer.setFont(fontValue);
        }
        Boolean booleanValue = (Boolean) UIManager.get("Tree.drawsFocusBorderAroundIcon");

        checkboxRenderer.setFocusPainted((booleanValue != null) && (booleanValue));

        selectionBorderColor = UIManager.getColor("Tree.selectionBorderColor");
        selectionForeground = UIManager.getColor("Tree.selectionForeground");
        selectionBackground = UIManager.getColor("Tree.selectionBackground");
        textForeground = UIManager.getColor("Tree.textForeground");
        textBackground = UIManager.getColor("Tree.textBackground");
    }

    protected JCheckBox getLeafRenderer() {
        return checkboxRenderer;
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean selected, boolean expanded, boolean leaf, int row,
                                                  boolean hasFocus) {

        Component returnValue;
        String stringValue = tree.convertValueToText(value, selected,
                expanded, leaf, row, false);
        checkboxRenderer.setText(stringValue);
        checkboxRenderer.setEnabled(tree.isEnabled());
        if (selected) {
            checkboxRenderer.setForeground(selectionForeground);
            checkboxRenderer.setBackground(selectionBackground);
        } else {
            checkboxRenderer.setForeground(textForeground);
            checkboxRenderer.setBackground(textBackground);
        }

        if (leaf) {
            if (value instanceof MethodCallExpression) {
                boolean isSelected = ((MethodCallExpression) value).isUIselected();
                checkboxRenderer.setSelected(isSelected);
            }
            returnValue = checkboxRenderer;
        } else {
            if (!(value instanceof TestCandidateTreeModel.RootNode)) //not root
            {
                if (value instanceof TestCandidateMetadata) {
                    boolean isSelected = ((TestCandidateMetadata) value).isUIselected();
                    checkboxRenderer.setSelected(isSelected);
                }
                returnValue = checkboxRenderer;
            } else {
                rootRenderer.setOpenIcon(icon);
                rootRenderer.setClosedIcon(icon);
                returnValue = rootRenderer.getTreeCellRendererComponent(tree,
                        value, selected, expanded, leaf, row, hasFocus);
            }
        }
        return returnValue;
    }
}


