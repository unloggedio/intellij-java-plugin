package com.insidious.plugin.ui;

import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.JsonFramework;
import com.insidious.plugin.pojo.TestFramework;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class TestCandidateCustomizeView {

    private final TestCandidateMetadata testCandidateMetadata;
    private final SessionInstance sessionInstance;
    private final TestCaseGenerationConfiguration testGenerationConfiguration;
    private TestGenerateActionListener testActionListener;
    private JPanel treeContainer;
    private JPanel mainPanel;
    private JTree testCandidateTree;
    private JPanel treeControlPanel;
    private JScrollPane treeScroller;
    private JButton generateButton;
    private JButton cancelButton;
    private JPanel configPanel;
    private JPanel buttonsPanel;
    private JLabel descriptionText;
    private JTextPane documentationTextArea;
    private CustomizeViewTreeCellRenderer cellRenderer;

    public TestCandidateCustomizeView(
            TestCandidateMetadata testCandidateMetadata,
            SessionInstance sessionInstance,
            TestGenerateActionListener testActionListener
    ) {
        this.testCandidateMetadata = testCandidateMetadata;
        this.sessionInstance = sessionInstance;
        this.testActionListener = testActionListener;
        this.testGenerationConfiguration = new TestCaseGenerationConfiguration();

        testGenerationConfiguration.setTestFramework(TestFramework.JUNIT5);
        testGenerationConfiguration.setJsonFramework(JsonFramework.GSON);

        TestCandidateTreeModel candidateTree = new TestCandidateTreeModel(
                testCandidateMetadata, testGenerationConfiguration, sessionInstance);
        this.testCandidateTree.setModel(candidateTree);

        cellRenderer = new CustomizeViewTreeCellRenderer();
        this.testCandidateTree.setCellRenderer(cellRenderer);

        //removeDefaultSelectionListeners();
        this.testCandidateTree.setSelectionModel(new CustomCheckboxSelectionModel());
        setDefaultSelection();

        generateButton.addActionListener((e) -> generateWithSelectedOptions());
        cancelButton.addActionListener((e) -> cancelAndBack());
    }

//    private void removeDefaultSelectionListeners()
//    {
//        TreeSelectionListener[] listeners = this.testCandidateTree.getTreeSelectionListeners();
//        for (int i=0;i<listeners.length;i++)
//        {
//            //System.out.println("Listener - "+i+", "+listeners[i].toString());
//            this.testCandidateTree.removeTreeSelectionListener(listeners[i]);
//        }
//    }

    private void setDefaultSelection()
    {
        int level1_rowcount = this.testCandidateTree.getRowCount();
        //select all l1 nodes
        for(int i=0; i<level1_rowcount; i++){
            TreePath path = this.testCandidateTree.getPathForRow(i);
            this.testCandidateTree.addSelectionPath(path);
        }
        //select the first and last nodes of each row
        TestCandidateTreeModel model = (TestCandidateTreeModel) this.testCandidateTree.getModel();

        ArrayList<TreePath> leafPaths = new ArrayList<TreePath>();
        TreePath[] paths = this.testCandidateTree.getSelectionPaths();

        for(int i=1;i< paths.length;i++)
        {
            Object selectedNode = paths[i].getLastPathComponent();
            int count = model.getChildCount(selectedNode);

            try {

                leafPaths.add(paths[i].pathByAddingChild(model.getChild(selectedNode, 0)));
                leafPaths.add(paths[i].pathByAddingChild(model.getChild(selectedNode, count - 1)));
            }
            catch (Exception e)
            {
                System.out.println("Exception e -> "+e);
            }
        }
        for(TreePath path : leafPaths)
        {
            this.testCandidateTree.addSelectionPath(path);
        }
    }

    private void printSelections()
    {
        TreePath[] paths = this.testCandidateTree.getSelectionPaths();
        for(int i=0;i<paths.length;i++)
        {
            System.out.println("Selection : "+i+" "+paths[i].toString());
        }
    }

    private void cancelAndBack() {
        testActionListener.cancel();
    }

    private void generateWithSelectedOptions() {
        printSelections();
        testActionListener.generateTestCase(testGenerationConfiguration);
    }

    public JPanel getContentPanel() {
        return mainPanel;
    }

}

