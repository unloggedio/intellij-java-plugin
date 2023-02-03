package com.insidious.plugin.ui;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;

public class SingleWindowView {
    public static final String CLASSES_LABEL = "Classes";
    private static final Logger logger = LoggerUtil.getInstance(SingleWindowView.class);
    private final Project project;
    private final InsidiousService insidiousService;
    private final GridConstraints constraints;
    private JButton refreshButton;
    private JPanel mainPanel;
    private JPanel filterPanel;
    private JPanel eventViewerPanel;
    private EventLogWindow eventViewer;

    public SingleWindowView(Project project, InsidiousService insidiousService) {

        this.project = project;


        refreshButton.addActionListener(e -> {
            try {
                generateAllTestCandidateCases();
            } catch (Throwable e1) {
                e1.printStackTrace();
            }
        });
        this.insidiousService = insidiousService;
        constraints = new GridConstraints();
        constraints.setFill(GridConstraints.FILL_BOTH);

        eventViewer = new EventLogWindow(insidiousService);
        eventViewerPanel.add(eventViewer.getContent(), constraints);

    }

    public void generateAllTestCandidateCases() throws Exception {
        insidiousService.generateAllTestCandidateCases();

    }

    public JComponent getContent() {
        return mainPanel;
    }

}
