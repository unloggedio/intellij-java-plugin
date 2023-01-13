package com.insidious.plugin.ui;

import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.io.IOException;

public class SingleWindowView {
    public static final String CLASSES_LABEL = "Classes";
    private final Project project;
    private final InsidiousService insidiousService;
    private static final Logger logger = LoggerUtil.getInstance(SingleWindowView.class);
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
//        mainTree.addTreeExpansionListener(SingleWindowView.this);
//        mainTree.addTreeWillExpandListener(SingleWindowView.this);
//        mainTree.addTreeSelectionListener(SingleWindowView.this);
//        treeModel = new VideobugTreeModel(insidiousService);
//        mainTree.setModel(treeModel);

//        mainTree.setCellRenderer(cellRenderer);
//        TreeUtil.installActions(mainTree);

//        resultPanel.setDividerLocation(0.99d);
        constraints = new GridConstraints();
        constraints.setFill(GridConstraints.FILL_BOTH);

        eventViewer = new EventLogWindow(insidiousService);
        eventViewerPanel.add(eventViewer.getContent(), constraints);

    }

    public void generateAllTestCandidateCases() {
//        treeModel = new VideobugTreeModel(insidiousService);
//        mainTree.setModel(treeModel);

    }

    public JComponent getContent() {
        return mainPanel;
    }

}
