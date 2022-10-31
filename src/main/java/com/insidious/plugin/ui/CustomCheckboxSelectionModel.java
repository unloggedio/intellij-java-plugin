package com.insidious.plugin.ui;

import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;

public class CustomCheckboxSelectionModel extends DefaultTreeSelectionModel {

    public void setSelectionPath(TreePath path){
        addSelectionPath(path);
    }

    @Override
    public void addSelectionPath(TreePath path) {
        super.addSelectionPath(path);
    }

    @Override
    public void addSelectionPaths(TreePath[] paths) {
        if (paths != null) {
            for (TreePath path : paths) {
                TreePath[] toAdd = new TreePath[1];
                toAdd[0] = path;

                if (isPathSelected(path)) {
                    super.removeSelectionPaths(toAdd);
                } else {
                    super.addSelectionPaths(toAdd);
                }
            }
        }
    }
}