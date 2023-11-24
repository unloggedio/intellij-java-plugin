package com.insidious.plugin.ui.eventviewer;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.TestCaseUtils;
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
    private final EventLogWindow eventViewer;
    private JButton refreshButton;
    private JPanel mainPanel;
    private JPanel filterPanel;
    private JPanel eventViewerPanel;

    public SingleWindowView(Project project) {

        this.project = project;


        refreshButton.addActionListener(e -> {
            try {
                generateAllTestCandidateCases();
            } catch (Throwable e1) {
                e1.printStackTrace();
            }
        });
        insidiousService = project.getService(InsidiousService.class);
        constraints = new GridConstraints();
        constraints.setFill(GridConstraints.FILL_BOTH);

        eventViewer = new EventLogWindow(insidiousService.getProject(), insidiousService.getClient());
        eventViewerPanel.add(eventViewer.getContent(), constraints);

    }

    public void generateAllTestCandidateCases() throws Exception {
        TestCaseUtils.generateAllTestCandidateCases(insidiousService, insidiousService.getClient());
    }

    public JComponent getContent() {
        return mainPanel;
    }

}
