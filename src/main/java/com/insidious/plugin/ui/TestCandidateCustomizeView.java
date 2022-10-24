package com.insidious.plugin.ui;

import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;

import javax.swing.*;
import java.awt.*;

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
            TestGenerateActionListener testActionListener) {
        this.testCandidateMetadata = testCandidateMetadata;
        this.sessionInstance = sessionInstance;
        this.testActionListener = testActionListener;
        this.testGenerationConfiguration = new TestCaseGenerationConfiguration();


        TestCandidateTreeModel candidateTree = new TestCandidateTreeModel(testCandidateMetadata, sessionInstance);
        this.testCandidateTree.setModel(candidateTree);

        cellRenderer = new CustomizeViewTreeCellRenderer();
        this.testCandidateTree.setCellRenderer(cellRenderer);

        generateButton.setBackground(Color.RED);
        generateButton.setOpaque(true);
        generateButton.setBorderPainted(false);
        generateButton.setContentAreaFilled(true);
        generateButton.addActionListener((e) -> generateWithSelectedOptions());
        cancelButton.addActionListener((e) -> cancelAndBack());
    }

    private void cancelAndBack() {
        testActionListener.cancel();
    }

    private void generateWithSelectedOptions() {
        testActionListener.generateTestCase(testCandidateMetadata, testGenerationConfiguration);
    }

    public JPanel getContentPanel() {
        return mainPanel;
    }
}
