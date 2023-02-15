package com.insidious.plugin.ui;

import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.factory.testcase.TestCaseService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.TimeUnit;

public class TestCandidateMetadataView {
    private final TestCandidateMetadata testCandidateMetadata;
    private final TestCaseService testCaseService;
    private final TestSelectionListener candidateSelectionListener;
    private JPanel contentPanel;
    private JLabel testCandidateName;
    private JButton generateTestCaseButton;
    private JPanel labelPanel;
    private JPanel buttonPanel;
    private JLabel candidateNumber;
    private JPanel cardPanel;
    private JPanel borderParent;
    private Dimension contentPanelDimensions = new Dimension(-1, 60);

    public TestCandidateMetadataView(
            TestCandidateMetadata testCandidateMetadata,
            TestCaseService testCaseService,
            TestSelectionListener candidateSelectionListener
    ) {
        this.testCandidateMetadata = testCandidateMetadata;
        this.testCaseService = testCaseService;
        this.candidateSelectionListener = candidateSelectionListener;
        this.contentPanel.setMaximumSize(contentPanelDimensions);
        this.contentPanel.setMinimumSize(contentPanelDimensions);
        this.contentPanel.setPreferredSize(contentPanelDimensions);

        this.generateTestCaseButton.setContentAreaFilled(false);
        this.generateTestCaseButton.setOpaque(false);
        this.generateTestCaseButton.setBorderPainted(false);

        long callTime = testCandidateMetadata.getCallTimeNanoSecond();
        long timeInMs = TimeUnit.NANOSECONDS.toMillis(callTime);
        this.candidateNumber.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        this.contentPanel.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent me) {
                hoverStateManager(me, true);
            }

            public void mouseExited(MouseEvent me) {
                hoverStateManager(me, false);
            }
        });

        testCandidateName.setText(timeInMs + " ms");
        generateTestCaseButton.addActionListener(e -> loadInputOutputInformation());
        contentPanel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                loadInputOutputInformation();
            }
        });
        generateTestCaseButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    }

    private void generateTestCase() {
        candidateSelectionListener.onSelect(testCandidateMetadata);
    }

    private void loadInputOutputInformation() {
        setSelectedState(true);
        candidateSelectionListener.loadInputOutputInformation(testCandidateMetadata);
    }

    public Component getContentPanel() {
        return contentPanel;
    }

    public void setCandidateNumberIndex(int candidateNumberIndex) {
        this.candidateNumber.setText("Candidate " + candidateNumberIndex);
    }

    public void setSelectedState(boolean status) {
        if (status) {
            Border border = new LineBorder(UIUtils.teal);
            contentPanel.setBorder(border);
        } else {
            Color color = new Color(187, 187, 187);
            Border border = new LineBorder(color);
            contentPanel.setBorder(border);
        }
    }

    private void hoverStateManager(MouseEvent me, boolean mouseEntered) {
        if (mouseEntered) {
            Color color = new Color(1, 204, 245);
            Border border = new LineBorder(color);
            contentPanel.setBorder(border);
            this.labelPanel.setOpaque(true);
            this.contentPanel.setOpaque(true);
            this.cardPanel.setOpaque(true);
            this.buttonPanel.setOpaque(true);
            this.borderParent.setOpaque(true);
            Color transparent = new Color(1, 204, 245, 1);
            Color transparent_base = new Color(1, 204, 245, 50);
            this.cardPanel.setBackground(transparent);
            this.buttonPanel.setBackground(transparent);
            this.borderParent.setBackground(transparent);
            this.labelPanel.setBackground(transparent);
            this.contentPanel.setBackground(transparent_base);
        } else {
            Color color = new Color(187, 187, 187);
            Border border = new LineBorder(color);
            contentPanel.setBorder(border);
            this.labelPanel.setOpaque(false);
            this.cardPanel.setOpaque(false);
            this.contentPanel.setOpaque(false);
        }
    }

}
