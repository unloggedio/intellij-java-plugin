package com.insidious.plugin.ui;

import com.insidious.plugin.util.UIUtils;
import com.intellij.openapi.util.IconLoader;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public class IOTreeCellRenderer implements TreeCellRenderer {

    private final JLabel labelRender = new JLabel();
    public IOTreeCellRenderer()
    {
        labelRender.setForeground(UIUtils.inputViewerTreeForeground);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        labelRender.setText(value.toString());
        return labelRender;
    }
}
