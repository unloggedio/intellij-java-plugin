package com.insidious.plugin.ui;

import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.*;
import com.intellij.notification.NotificationType;
import org.antlr.v4.runtime.tree.Tree;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

        this.testCandidateTree.setToggleClickCount(0);
        setDefaultSelection();
        setDocumentationText();
        setDividerColor();

        this.testCandidateTree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                handleSelections(me);
            }
        });

        generateButton.addActionListener((e) -> generateWithSelectedOptions());
        cancelButton.addActionListener((e) -> cancelAndBack());
    }

    private void handleSelections(MouseEvent me)
    {
        TreePath lastPath = this.testCandidateTree.getPathForLocation(me.getX(), me.getY());
        if(lastPath==null)
        {
            return;
        }
        if(lastPath.getPathCount()==2)
        {
            TestCandidateMetadata metadata = (TestCandidateMetadata) lastPath.getLastPathComponent();
            metadata.setUIselected(!metadata.isUIselected());
            if(this.testGenerationConfiguration.getTestCandidateMetadataList().contains(metadata))
            {
                if(!metadata.isUIselected())
                {
                    this.testGenerationConfiguration.getTestCandidateMetadataList().remove(metadata);
                }
            }
            else
            {
                if(metadata.isUIselected())
                {
                    this.testGenerationConfiguration.getTestCandidateMetadataList().add(metadata);
                }
            }
            bulkSetCallStatus(metadata,metadata.isUIselected());
        }
        else if(lastPath.getPathCount()==3)
        {
            MethodCallExpression mce = (MethodCallExpression) lastPath.getLastPathComponent();
            mce.setUIselected(!mce.isUIselected());
            if(this.testGenerationConfiguration.getCallExpressionList().contains(mce))
            {
                if(!mce.isUIselected())
                {
                    this.testGenerationConfiguration.getCallExpressionList().remove(mce);
                }
            }
            else
            {
                if(mce.isUIselected())
                {
                    this.testGenerationConfiguration.getCallExpressionList().add(mce);
                }
            }
        }
        refreshTree();
        this.testCandidateTree.scrollPathToVisible(lastPath);
    }

    private void bulkSetCallStatus(TestCandidateMetadata metadata, boolean status)
    {
        List<MethodCallExpression> calls = metadata.getCallsList();
        for(MethodCallExpression call : calls)
        {
            call.setUIselected(status);
            if(this.testGenerationConfiguration.getCallExpressionList().contains(call))
            {
                this.testGenerationConfiguration.getCallExpressionList().remove(call);
            }
            else
            {
                this.testGenerationConfiguration.getCallExpressionList().add(call);
            }
        }
    }

    private void refreshTree()
    {
        List<TreePath> paths = Arrays.asList(this.testCandidateTree.getSelectionPaths());
        if(paths.contains(this.testCandidateTree.getPathForRow(0)))
        {
            this.testCandidateTree.removeSelectionPath(this.testCandidateTree.getPathForRow(0));
        }
        else
        {
            this.testCandidateTree.addSelectionPath(this.testCandidateTree.getPathForRow(0));
            this.testCandidateTree.removeSelectionPath(this.testCandidateTree.getPathForRow(0));
        }
    }

    private void setDefaultSelection() {
        int level1_rowcount = this.testCandidateTree.getRowCount();
        try {
            TestCandidateMetadata firstCandidate = (TestCandidateMetadata) this.testCandidateTree.getPathForRow(1).getLastPathComponent();
            TestCandidateMetadata lastCandidate = (TestCandidateMetadata) this.testCandidateTree.getPathForRow(level1_rowcount - 1).getLastPathComponent();
            firstCandidate.setUIselected(true);
            lastCandidate.setUIselected(true);
            this.testGenerationConfiguration.getTestCandidateMetadataList().add(firstCandidate);
            this.testGenerationConfiguration.getTestCandidateMetadataList().add(lastCandidate);
            bulkSetCallStatus(firstCandidate,firstCandidate.isUIselected());
            bulkSetCallStatus(lastCandidate,lastCandidate.isUIselected());

            //refreshTree();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception selecting default candidate nodes");
        }
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
        this.testGenerationConfiguration.setTestCandidateMetadataList(refCandidates_order);
    }

    private void generateWithSelectedOptions() {
        sortCandidates();
        printConfigDetails();
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
}

