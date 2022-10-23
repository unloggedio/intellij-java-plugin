package com.insidious.plugin.ui;

import com.insidious.plugin.factory.testcase.TestCaseService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.MethodCallExpression;

import javax.swing.*;
import java.awt.*;

public class TestCandidateMetadataView {
    private final TestCandidateMetadata testCandidateMetadata;
    private final TestCaseService testCaseService;
    private final TestSelectionListener candidateSelectionListener;
    private JPanel contentPanel;
    private JLabel testCandidateName;
    private JButton generateTestCaseButton;
    private JPanel labelPanel;
    private JPanel buttonPanel;

    private Dimension contentPanelDimensions = new Dimension(-1,30);

    public TestCandidateMetadataView(
            TestCandidateMetadata testCandidateMetadata,
            TestCaseService testCaseService,
            TestSelectionListener candidateSelectionListener
    ) {
        this.testCandidateMetadata = testCandidateMetadata;
        this.testCaseService = testCaseService;
        this.candidateSelectionListener = candidateSelectionListener;
        this.contentPanel.setMaximumSize(contentPanelDimensions);
        this.contentPanel.setMaximumSize(contentPanelDimensions);
        MethodCallExpression mainMethod = (MethodCallExpression) testCandidateMetadata.getMainMethod();
        testCandidateName.setText(mainMethod.getMethodName() + " at " + mainMethod.getEntryProbe().getNanoTime());
        generateTestCaseButton.addActionListener(e -> generateTestCase());
    }

    private void generateTestCase() {
        candidateSelectionListener.onSelect(testCandidateMetadata);
    }

    public Component getContentPanel() {
        return contentPanel;
    }

    public Dimension getContentPanelDimensions() {
        return contentPanelDimensions;
    }

    public void setContentPanelDimensions(Dimension contentPanelDimensions) {
        this.contentPanelDimensions = contentPanelDimensions;
    }
}
