package com.insidious.plugin.ui;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public class IOTreeCellRenderer implements TreeCellRenderer {

    Icon noIconRef = IconLoader.getIcon("icons/png/transparent.png", IOTreeCellRenderer.class);
    Icon topLevelIcon = IconLoader.getIcon("icons/png/IOpointer.png", IOTreeCellRenderer.class);
    private DefaultTreeCellRenderer defaultTreeCellRenderer = new DefaultTreeCellRenderer();

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

        if(value instanceof DefaultMutableTreeNode)
        {
            Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            if(userObject instanceof CandidateInformationWindow.ParameterInformation)
            {
                defaultTreeCellRenderer.setClosedIcon(topLevelIcon);
                defaultTreeCellRenderer.setOpenIcon(topLevelIcon);
                defaultTreeCellRenderer.setLeafIcon(topLevelIcon);
            }
            else
            {
                defaultTreeCellRenderer.setClosedIcon(noIconRef);
                defaultTreeCellRenderer.setOpenIcon(noIconRef);
                defaultTreeCellRenderer.setLeafIcon(noIconRef);
            }
        }

        return defaultTreeCellRenderer.getTreeCellRendererComponent(tree,
                value, selected, expanded, leaf, row, hasFocus);
    }
}
