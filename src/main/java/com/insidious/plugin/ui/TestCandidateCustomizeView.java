package com.insidious.plugin.ui;

import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.*;
import com.intellij.notification.NotificationType;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.tree.TreePath;
import java.awt.*;
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
    private JSplitPane splitPane;
    private JScrollPane scrollParent;
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
        this.testCandidateTree.setToggleClickCount(0);
        this.testCandidateTree.addTreeWillExpandListener(treeWillExpandListener);
        this.testCandidateTree.setSelectionModel(new CustomCheckboxSelectionModel(candidateTree,testGenerationConfiguration));
        setDefaultSelection();

        setDocumentationText();
        setDividerColor();

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
        System.out.println("Selections - ");
        TreePath[] paths = this.testCandidateTree.getSelectionPaths();
        for (int i = 0; i < paths.length; i++) {
            System.out.println("Selection : " + i + " " + paths[i].toString());
        }
    }

    private void cancelAndBack() {
        testActionListener.cancel();
    }

    private void printConfigDetails()
    {
        System.out.println("[Candidates length ]"+this.testGenerationConfiguration.getTestCandidateMetadataList().size());
        System.out.println("[Config - Candidates] "+this.testGenerationConfiguration.getTestCandidateMetadataList().toString());
        System.out.println("[Calls length ]"+this.testGenerationConfiguration.getCallExpressionList().size());
        System.out.println("[Config - Calls] "+this.testGenerationConfiguration.getCallExpressionList().toString());
    }

    private void sortCandidates()
    {
        TestCandidateTreeModel model = (TestCandidateTreeModel) this.testCandidateTree.getModel();
        List<TestCandidateMetadata> refCandidates_order = new ArrayList<>();
        refCandidates_order.addAll(model.getCandidateList());

        List<TestCandidateMetadata> fallback= this.testGenerationConfiguration.getTestCandidateMetadataList();
        refCandidates_order.removeIf(p -> !fallback.contains(p));
//        System.out.println("[Sorted] "+refCandidates_order.toString());
        this.testGenerationConfiguration.setTestCandidateMetadataList(refCandidates_order);
    }

    private void generateWithSelectedOptions() {
        printConfigDetails();
        sortCandidates();
        try {
            testActionListener.generateTestCase(testGenerationConfiguration);
        } catch (Exception e) {
            e.printStackTrace();
            InsidiousNotification.notifyMessage("Failed to generate test case - " + e.getMessage(), NotificationType.ERROR);

            JSONObject eventProperties = new JSONObject();
            eventProperties.put("message", e.getMessage());
            UsageInsightTracker.getInstance().RecordEvent("TestCaseGenerationFailed", eventProperties);
        }
    }

    public JPanel getContentPanel() {
        return mainPanel;
    }

    public void setDocumentationText() {
        TestCandidateTreeModel model = (TestCandidateTreeModel) this.testCandidateTree.getModel();
        this.documentationTextArea.setText(model.getDocumentationText());
    }

    private void setDividerColor() {
        splitPane.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    public void setBorder(Border b) {
                    }

                    @Override
                    public void paint(Graphics g) {
                        Color teal = new Color(1, 204, 245);
                        g.setColor(teal);
                        g.fillRect(0, 0, getSize().width, getSize().height);
                        super.paint(g);
                    }
                };
            }
        });
    }

    TreePath[] fallBackPaths = null;
    TreeWillExpandListener treeWillExpandListener = new TreeWillExpandListener() {
        public void treeWillCollapse(TreeExpansionEvent treeExpansionEvent) {
            fallBackPaths = testCandidateTree.getSelectionPaths();
        }

        public void treeWillExpand(TreeExpansionEvent treeExpansionEvent) {
            if(fallBackPaths!=null)
            {
                testCandidateTree.setSelectionPaths(fallBackPaths);
            }
        }
    };
}

