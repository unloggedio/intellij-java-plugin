package com.insidious.plugin.ui;

import javax.swing.*;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

public class CustomCheckboxSelectionModel extends DefaultTreeSelectionModel {

    private TestCandidateTreeModel treeModel;

    public CustomCheckboxSelectionModel(TestCandidateTreeModel model)
    {
        this.treeModel = model;
    }
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
                    if(path.getPathCount()==2)
                    {
                        manageSelections(path,false);
                    }
                    super.removeSelectionPaths(toAdd);
                } else {
                    if(path.getPathCount()==2)
                    {
                        manageSelections(path,true);
                    }
                    super.addSelectionPaths(toAdd);
                }
            }
        }
    }

    public void manageSelections(TreePath parent, boolean add)
    {
        List<TreePath> children = new ArrayList<TreePath>();
        Object candidate = parent.getLastPathComponent();
        int childCount = treeModel.getChildCount(candidate);
        //System.out.println("In manage selection for path -> "+parent.toString()+", child count : "+childCount);
        for(int i=0;i<childCount;i++)
        {
            if(add)
            {
                //System.out.println("Adding new child path for -> "+parent.toString());
                TreePath[] toAdd = new TreePath[1];
                toAdd[0] = parent.pathByAddingChild(treeModel.getChild(candidate, i));
                super.addSelectionPaths(toAdd);
            }
            else
            {
                //System.out.println("Removing child path for -> "+parent.toString());
                TreePath[] toRemove = new TreePath[1];
                toRemove[0] = parent.pathByAddingChild(treeModel.getChild(candidate, i));
                super.removeSelectionPaths(toRemove);
            }
        }
    }


}