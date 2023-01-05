package com.insidious.plugin.ui;

import com.insidious.plugin.client.TestCandidateMethodAggregate;
import com.insidious.plugin.client.VideobugTreeClassAggregateNode;
import com.insidious.plugin.client.VideobugTreePackageAggregateNode;
import com.intellij.openapi.util.IconLoader;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public class VideobugTreeCellRenderer implements TreeCellRenderer {
    private final DefaultTreeCellRenderer fallback;
    private final Icon methodIcon;
    private final Icon classIcon;
    private final Icon packageIcon;
    private Icon errorIcon;

    public VideobugTreeCellRenderer() {
        this.fallback = new DefaultTreeCellRenderer();

        this.methodIcon = IconLoader.getIcon("icons/png/method_v1.png", VideobugTreeCellRenderer.class);
        this.packageIcon = IconLoader.getIcon("icons/png/package_v1.png", VideobugTreeCellRenderer.class);
        this.classIcon = IconLoader.getIcon("icons/png/class_v1.png", VideobugTreeCellRenderer.class);
        this.errorIcon = IconLoader.getIcon("/icons/png/load_Error.png", VideobugTreeCellRenderer.class);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object userObject, boolean sel, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {

        Component component;
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();

        if(userObject instanceof VideobugTreePackageAggregateNode)
        {
            renderer.setClosedIcon(packageIcon);
            renderer.setOpenIcon(packageIcon);
        }
        else if(userObject instanceof VideobugTreeClassAggregateNode)
        {
            renderer.setClosedIcon(classIcon);
            renderer.setOpenIcon(classIcon);
        }
        else if(userObject instanceof TestCandidateMethodAggregate)
        {
            renderer.setLeafIcon(methodIcon);
        }
        else if(userObject instanceof DefaultMutableTreeNode)
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) userObject;
            if(node.getUserObject() instanceof StringBuilder)
            {
                renderer.setLeafIcon(packageIcon);
            }
            else
            {
                renderer.setLeafIcon(errorIcon);
            }
        }

        component = renderer.getTreeCellRendererComponent(tree, userObject, sel, expanded, leaf, row, hasFocus);
        return component;
    }
}
