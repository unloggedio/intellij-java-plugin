package com.insidious.plugin.ui.methodscope;

import javax.swing.*;

public class CoveragePanel {
    private JPanel rootPanel;
    private JPanel titlePanel;
    private JPanel coverageContentPanel;
    private JProgressBar progressBar1;
    private JPanel progressBarContainerPanel;
    private JPanel controlPanel;
    private JButton viewBreakdownButton;
    private JPanel highlightControlPanel;
    private JRadioButton onRadioButton;
    private JRadioButton offRadioButton;
    private JLabel percentLabel;
    private JPanel pendingCoverageMessagePanel;
    private JLabel potentialCoverageInfoLabel;
    private JLabel potentialNewPercent;

    public JPanel getContent() {
        return rootPanel;
    }

    public void setCoverageData(int totalLineCount, int coveredSavedLineCount, int coveredUnsavedLineCount) {
        pendingCoverageMessagePanel.setVisible(false);
        if (totalLineCount > 0) {
            int coverageSavedPercent = (coveredSavedLineCount * 100) / totalLineCount;
            progressBar1.setValue(coverageSavedPercent);
            percentLabel.setText(coverageSavedPercent + "%");
            int coverageUnsavedPercent = (coveredUnsavedLineCount * 100) / totalLineCount;
            if (coverageUnsavedPercent > 0) {
                int totalFinalPercent = coverageUnsavedPercent + coverageSavedPercent;
                potentialNewPercent.setText(totalFinalPercent + "%");
                pendingCoverageMessagePanel.setVisible(true);
            }
        } else {
            progressBar1.setValue(0);
            percentLabel.setText("Unavailable");
        }

    }
}
