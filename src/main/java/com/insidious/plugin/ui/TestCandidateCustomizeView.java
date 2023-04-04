package com.insidious.plugin.ui;

import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.pojo.*;
import com.intellij.notification.NotificationType;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
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
                TestFramework.JUnit5, MockFramework.Mockito, JsonFramework.GSON, ResourceEmbedMode.IN_FILE);

        UsageInsightTracker.getInstance().
                RecordEvent("CustomizeViewLoaded", null);

        TestCandidateTreeModel candidateTree = new TestCandidateTreeModel(
                testCandidateMetadata, testGenerationConfiguration, sessionInstance);
        this.testCandidateTree.setModel(candidateTree);

        cellRenderer = new CustomizeViewTreeCellRenderer();
        this.testCandidateTree.setCellRenderer(cellRenderer);

        this.testCandidateTree.setToggleClickCount(0);
        setDefaultSelection();
        setDocumentationText();
        UIUtils.setDividerColorForSplitPane(splitPane, UIUtils.teal);

        this.testCandidateTree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                handleSelections(me);
            }
        });

        generateButton.addActionListener((e) -> generateWithSelectedOptions());
        generateButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelButton.addActionListener((e) -> cancelAndBack());
        cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void handleSelections(MouseEvent me) {
        TreePath lastPath = this.testCandidateTree.getPathForLocation(me.getX(), me.getY());
        if (lastPath == null) {
            return;
        }
        if (lastPath.getPathCount() == 2) {
            TestCandidateMetadata metadata = (TestCandidateMetadata) lastPath.getLastPathComponent();
            metadata.setUIselected(!metadata.isUIselected());
            if (this.testGenerationConfiguration.getTestCandidateMetadataList()
                    .contains(metadata)) {
                if (!metadata.isUIselected()) {
                    this.testGenerationConfiguration.getTestCandidateMetadataList()
                            .remove(metadata);
                }
            } else {
                if (metadata.isUIselected()) {
                    this.testGenerationConfiguration.getTestCandidateMetadataList()
                            .add(metadata);
                }
            }
            bulkSetCallStatus(metadata, metadata.isUIselected());
        } else if (lastPath.getPathCount() == 3) {
            MethodCallExpression mce = (MethodCallExpression) lastPath.getLastPathComponent();
            mce.setUIselected(!mce.isUIselected());
            if (this.testGenerationConfiguration.getCallExpressionList()
                    .contains(mce)) {
                if (!mce.isUIselected()) {
                    this.testGenerationConfiguration.getCallExpressionList()
                            .remove(mce);
                }
            } else {
                if (mce.isUIselected()) {
                    this.testGenerationConfiguration.getCallExpressionList()
                            .add(mce);
                }
            }
        }
        refreshTree();
        this.testCandidateTree.scrollPathToVisible(lastPath);
    }

    private void bulkSetCallStatus(TestCandidateMetadata metadata, boolean status) {
        List<MethodCallExpression> calls = metadata.getCallsList();
        for (MethodCallExpression call : calls) {
            call.setUIselected(status);
            if (status) {
                this.testGenerationConfiguration.getCallExpressionList()
                        .add(call);
            } else {
                this.testGenerationConfiguration.getCallExpressionList()
                        .remove(call);
            }
        }
    }

    private void refreshTree() {
        this.testCandidateTree.repaint();
    }

    private void setDefaultSelection() {
        int level1_rowcount = this.testCandidateTree.getRowCount();
        try {
            TestCandidateMetadata firstCandidate = (TestCandidateMetadata) this.testCandidateTree.getPathForRow(1)
                    .getLastPathComponent();
            TestCandidateMetadata lastCandidate = (TestCandidateMetadata) this.testCandidateTree.getPathForRow(
                            level1_rowcount - 1)
                    .getLastPathComponent();
            if (firstCandidate.getMainMethod().getMethodName().equals("<init>")) {
                firstCandidate.setUIselected(true);
                this.testGenerationConfiguration.getTestCandidateMetadataList()
                        .add(firstCandidate);
                bulkSetCallStatus(firstCandidate, firstCandidate.isUIselected());
            }
            lastCandidate.setUIselected(true);
            this.testGenerationConfiguration.getTestCandidateMetadataList()
                    .add(lastCandidate);
            bulkSetCallStatus(lastCandidate, lastCandidate.isUIselected());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception selecting default candidate nodes");
        }
    }

    private void cancelAndBack() {
        testActionListener.cancel();
    }

    private void printConfigDetails() {
        System.out.println("[Candidates length ]" + this.testGenerationConfiguration.getTestCandidateMetadataList()
                .size());
        System.out.println("[Config - Candidates] " + this.testGenerationConfiguration.getTestCandidateMetadataList()
                .toString());
        System.out.println("[Calls length ]" + this.testGenerationConfiguration.getCallExpressionList()
                .size());
        System.out.println("[Config - Calls] " + this.testGenerationConfiguration.getCallExpressionList()
                .toString());
    }

    private void sortCandidates() {
        TestCandidateTreeModel model = (TestCandidateTreeModel) this.testCandidateTree.getModel();
        List<TestCandidateMetadata> refCandidates_order = new ArrayList<>();
        refCandidates_order.addAll(model.getCandidateList());

        List<TestCandidateMetadata> fallback = this.testGenerationConfiguration.getTestCandidateMetadataList();
        refCandidates_order.removeIf(p -> !fallback.contains(p));
        this.testGenerationConfiguration.setTestCandidateMetadataList(refCandidates_order);
        this.testGenerationConfiguration.setTestMethodName(
                "testMethod" + ClassTypeUtils.upperInstanceName(
                        refCandidates_order.get(refCandidates_order.size() - 1).getMainMethod().getMethodName())
        );
    }

    private void generateWithSelectedOptions() {
        sortCandidates();
        try {
            testActionListener.generateTestCase(testGenerationConfiguration);
            cancelAndBack();
        } catch (Exception e) {
            e.printStackTrace();
            InsidiousNotification.notifyMessage("Testcase Generation failed."
                            + "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.",
                    NotificationType.ERROR);

            JSONObject eventProperties = new JSONObject();
            eventProperties.put("message", e.getMessage());
            UsageInsightTracker.getInstance()
                    .RecordEvent("TestCaseGenerationFailed", eventProperties);
        }
    }

    public JPanel getContentPanel() {
        return mainPanel;
    }

    public void setDocumentationText() {
        TestCandidateTreeModel model = (TestCandidateTreeModel) this.testCandidateTree.getModel();
        this.documentationTextArea.setText(model.getDocumentationText());
    }
}

