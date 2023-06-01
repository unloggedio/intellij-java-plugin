package com.insidious.plugin.ui;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public class IOTreeCellRenderer implements TreeCellRenderer {

    Icon noIconRef = IconLoader.getIcon("icons/png/transparent.png", IOTreeCellRenderer.class);
    Icon topLevelIcon = IconLoader.getIcon("icons/png/IOpointer.png", IOTreeCellRenderer.class);
    private final DefaultTreeCellRenderer defaultTreeCellRenderer = new DefaultTreeCellRenderer();

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        defaultTreeCellRenderer.setClosedIcon(null);
        defaultTreeCellRenderer.setOpenIcon(null);
        defaultTreeCellRenderer.setLeafIcon(null);
        Component treeCellRendererComponent = defaultTreeCellRenderer.getTreeCellRendererComponent(tree,
                value, selected, expanded, leaf, row, hasFocus);
        treeCellRendererComponent.setPreferredSize(new Dimension(-1, 10));
        return treeCellRendererComponent;
    }
}
