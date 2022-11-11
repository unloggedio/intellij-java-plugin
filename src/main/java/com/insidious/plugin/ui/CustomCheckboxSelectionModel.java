package com.insidious.plugin.ui;

import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.MethodCallExpression;

import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;

public class CustomCheckboxSelectionModel extends DefaultTreeSelectionModel {

    private TestCandidateTreeModel treeModel;
    private TestCaseGenerationConfiguration configuration;

    public CustomCheckboxSelectionModel(TestCandidateTreeModel model, TestCaseGenerationConfiguration configuration) {
        this.treeModel = model;
        this.configuration = configuration;
    }

    public void setSelectionPath(TreePath path) {
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

                if (path.getPathCount() == 1) {
                    if (isPathSelected(path)) {
                        super.removeSelectionPaths(toAdd);
                    }
                    return;
                }
                if (isPathSelected(path)) {
                    if (path.getPathCount() == 2) {
                        manageSelections(path, false);
                    }
                    super.removeSelectionPaths(toAdd);
                } else {
                    if (path.getPathCount() == 2) {
                        manageSelections(path, true);
                    }
                    super.addSelectionPaths(toAdd);
                }

                if (path.getPathCount() == 3) {
                    manageCallSelection(path);

                }
            }
        }
    }

    public void manageSelections(TreePath parent, boolean add) {
        Object candidate = parent.getLastPathComponent();
        TestCandidateMetadata metadata = (TestCandidateMetadata) candidate;

        if (this.configuration.getTestCandidateMetadataList().contains(metadata)) {
            this.configuration.getTestCandidateMetadataList().remove(metadata);
        } else {
            this.configuration.getTestCandidateMetadataList().add(metadata);
        }

        int childCount = treeModel.getChildCount(candidate);
        //System.out.println("In manage selection for path -> "+parent.toString()+", child count : "+childCount);
        for (int i = 0; i < childCount; i++) {
            if (add) {
//                System.out.println("Adding new child path for -> "+parent.toString());
                TreePath[] toAdd = new TreePath[1];
                toAdd[0] = parent.pathByAddingChild(treeModel.getChild(candidate, i));
                MethodCallExpression mce = (MethodCallExpression) treeModel.getChild(candidate, i);
                if (!configuration.getCallExpressionList().contains(mce)) {
//                    System.out.println("[config] Adding new child to selection for -> "+parent.toString());
                    this.configuration.getCallExpressionList().add(mce);
                }
                super.addSelectionPaths(toAdd);
            } else {
                System.out.println("Removing child path for -> " + parent.toString());
                TreePath[] toRemove = new TreePath[1];
                toRemove[0] = parent.pathByAddingChild(treeModel.getChild(candidate, i));
                MethodCallExpression mce = (MethodCallExpression) treeModel.getChild(candidate, i);
                if (configuration.getCallExpressionList().contains(mce)) {
                    System.out.println("[config] Removing child from selection for -> " + parent.toString());
                    this.configuration.getCallExpressionList().remove(mce);
                }
                super.removeSelectionPaths(toRemove);
            }
        }
    }

    private void manageCallSelection(TreePath path) {
        Object candidate = path.getLastPathComponent();
        MethodCallExpression mce = (MethodCallExpression) candidate;

        if (configuration.getCallExpressionList().contains(mce)) {
            System.out.println("[Removing] call for " + path.toString());
            configuration.getCallExpressionList().remove(mce);
            TreePath[] toRemove = new TreePath[1];
            toRemove[0] = path;
            super.removeSelectionPaths(toRemove);
        } else {
            System.out.println("[Adding] call for " + path.toString());
            configuration.getCallExpressionList().add(mce);
            TreePath[] toAdd = new TreePath[1];
            toAdd[0] = path;
            super.addSelectionPaths(toAdd);
        }

    }

}