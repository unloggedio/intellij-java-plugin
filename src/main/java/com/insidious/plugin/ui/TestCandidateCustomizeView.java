package com.insidious.plugin.ui;

import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.JsonFramework;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.TestFramework;
import com.intellij.notification.NotificationType;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

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

    private void setDefaultSelection() {
        int level1_rowcount = this.testCandidateTree.getRowCount();
        try {
            TreePath firstCandidate = this.testCandidateTree.getPathForRow(1);
            TreePath lastCandidate = this.testCandidateTree.getPathForRow(level1_rowcount - 1);
            this.testCandidateTree.addSelectionPaths(new TreePath[]{firstCandidate, lastCandidate});

        } catch (Exception e) {
            System.out.println("Exception selecting default candidate nodes");
            return;
        }
        //select the first and last nodes of each row
        TestCandidateTreeModel model = (TestCandidateTreeModel) this.testCandidateTree.getModel();

        ArrayList<TreePath> leafPaths = new ArrayList<TreePath>();
        TreePath[] paths = this.testCandidateTree.getSelectionPaths();

        for (int i = 0; i < paths.length; i++) {
            Object selectedNode = paths[i].getLastPathComponent();
            int count = model.getChildCount(selectedNode);

            try {
                for (int j = 0; j < count; j++) {
                    leafPaths.add(paths[i].pathByAddingChild(model.getChild(selectedNode, j)));
                }
            } catch (Exception e) {
                System.out.println("Exception e -> " + e);
            }
        }
        for (TreePath path : leafPaths) {
            this.testCandidateTree.addSelectionPath(path);
        }
    }

    private void printSelections() {
        TreePath[] paths = this.testCandidateTree.getSelectionPaths();
        for (int i = 0; i < paths.length; i++) {
            System.out.println("Selection : " + i + " " + paths[i].toString());
        }
    }

    private void setupGenerationConfiguration() {
        TreePath[] paths = this.testCandidateTree.getSelectionPaths();
        LinkedList<TestCandidateMetadata> candidates = new LinkedList<TestCandidateMetadata>();
        LinkedList<MethodCallExpression> calls = new LinkedList<MethodCallExpression>();
        for (TreePath path : paths) {
            //candidate metadata
            if (path.getPathCount() == 2) {
                TestCandidateMetadata candidate = (TestCandidateMetadata) path.getLastPathComponent();
                candidates.add(candidate);
            }
            //method call
            else if (path.getPathCount() == 3) {
                MethodCallExpression call = (MethodCallExpression) path.getLastPathComponent();
                calls.add(call);
            }
        }

        this.testGenerationConfiguration.setTestCandidateMetadataList(candidates);
        this.testGenerationConfiguration.setCallExpressionList(calls);
    }

    private void cancelAndBack() {
        testActionListener.cancel();
    }

    private void generateWithSelectedOptions() {
        printSelections();
//        setupGenerationConfiguration();
        try {
            testActionListener.generateTestCase(testGenerationConfiguration);
        } catch (IOException e) {
            e.printStackTrace();
            InsidiousNotification.notifyMessage("Failed to generate test case - " + e.getMessage(), NotificationType.ERROR);
        }
    }

    public JPanel getContentPanel() {
        return mainPanel;
    }

}

