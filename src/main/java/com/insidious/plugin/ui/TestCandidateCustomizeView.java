package com.insidious.plugin.ui;

import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.*;
import com.intellij.notification.NotificationType;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.io.IOException;
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
        this.testGenerationConfiguration = new TestCaseGenerationConfiguration(
                TestFramework.JUNIT5, MockFramework.MOCKITO, JsonFramework.GSON, ResourceEmbedMode.IN_FILE);

        TestCandidateTreeModel candidateTree = new TestCandidateTreeModel(
                testCandidateMetadata, testGenerationConfiguration, sessionInstance);
        this.testCandidateTree.setModel(candidateTree);

        cellRenderer = new CustomizeViewTreeCellRenderer();
        this.testCandidateTree.setCellRenderer(cellRenderer);

        //removeDefaultSelectionListeners();
        this.testCandidateTree.setSelectionModel(new CustomCheckboxSelectionModel(candidateTree));
        setDefaultSelection();

        setDocumentationText();

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
            e.printStackTrace();
            System.out.println("Exception selecting default candidate nodes");
        }
        //selection model selects all calls under candidate, no need to explicitly select them anymore
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

        System.out.println("Comparing Candidates ");
        System.out.println("[Existing candidates: ] "+this.testGenerationConfiguration.getTestCandidateMetadataList().toString());
        System.out.println("[New candidates: ] "+candidates.toString());
        if(this.testGenerationConfiguration.getTestCandidateMetadataList().equals(candidates))
        {
            System.out.println("[]Candidates are equal");
        }
        else
        {
            System.out.println("[]Candidates are not equal");
        }

        System.out.println("Comparing Calls ");
        System.out.println("[Existing Calls: ] "+this.testGenerationConfiguration.getCallExpressionList().toString());
        System.out.println("[New calls: ] "+calls.toString());
        if(this.testGenerationConfiguration.getCallExpressionList().equals(calls))
        {
            System.out.println("[]Calls are equal");
        }
        else
        {
            System.out.println("[]Calls are not equal");
        }

        this.testGenerationConfiguration.setTestCandidateMetadataList(candidates);
        this.testGenerationConfiguration.setCallExpressionList(calls);
    }

    private void cancelAndBack() {
        testActionListener.cancel();
    }

    private void generateWithSelectedOptions() {
        //printSelections();
        //setupGenerationConfiguration();
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

    public void setDocumentationText()
    {
        TestCandidateTreeModel model = (TestCandidateTreeModel) this.testCandidateTree.getModel();
        this.documentationTextArea.setText(model.getDocumentationText());
    }

}

