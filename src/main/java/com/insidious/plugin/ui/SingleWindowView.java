package com.insidious.plugin.ui;

import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.PageInfo;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.ExpandVetoException;
import java.io.IOException;

public class SingleWindowView implements TreeExpansionListener, TreeWillExpandListener, TreeSelectionListener {
    public static final String CLASSES_LABEL = "Classes";
    private final Project project;
    private final InsidiousService insidiousService;
    private final Logger logger = LoggerUtil.getInstance(SingleWindowView.class);
    private final VideobugTreeCellRenderer cellRenderer;
    private final GridConstraints constraints;
    private VideobugTreeModel treeModel;
    private JTree mainTree;
    private JButton refreshButton;
    private JPanel mainPanel;
    private JPanel filterPanel;
    private JSplitPane resultPanel;
    private JScrollPane treePanel;
    private JPanel infoPanelContainer;
    private JTextField textField1;
    private JPanel infoPanel;
    private JSplitPane infoPanelSplit;
    private JPanel eventViewerPanel;
    private SingleClassInfoWindow informationPanel;
    private JScrollPane detailedView;
    private EventLogWindow eventViewer;

    public SingleWindowView(Project project, InsidiousService insidiousService) {

        this.project = project;


        refreshButton.addActionListener(e -> {
            try {
                refresh();
            } catch (Throwable e1) {
                e1.printStackTrace();
            }
        });
        this.insidiousService = insidiousService;
        mainTree.addTreeExpansionListener(SingleWindowView.this);
        mainTree.addTreeWillExpandListener(SingleWindowView.this);
        mainTree.addTreeSelectionListener(SingleWindowView.this);

//        mainPanel.remove(detailedView);
        treeModel = new VideobugTreeModel(insidiousService);
        mainTree.setModel(treeModel);


        cellRenderer = new VideobugTreeCellRenderer(insidiousService);

        mainTree.setCellRenderer(cellRenderer);
        resultPanel.setDividerLocation(0.99d);
        TreeUtil.installActions(mainTree);

        constraints = new GridConstraints();
        constraints.setFill(GridConstraints.FILL_BOTH);
        refresh();

    }

    public void refresh() {
        treeModel = new VideobugTreeModel(insidiousService);
        mainTree.setModel(treeModel);

    }

    public JComponent getContent() {
        return mainPanel;
    }

    @Override
    public void treeExpanded(TreeExpansionEvent event) {
        logger.warn("Tree expansion event - " + event.getPath());
    }

    @Override
    public void treeCollapsed(TreeExpansionEvent event) {
        logger.warn("Tree collapse event - " + event.getPath());

    }


    @Override
    public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
        logger.warn("Tree will expand event - " + event.getPath());
    }

    @Override
    public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
        logger.warn("Tree treeWillCollapse event - " + event.getPath());

    }

    @Override
    public void valueChanged(TreeSelectionEvent event) {
        logger.warn("Tree valueChanged event - " + event.getPath());
        Object selectedNode = event.getPath().getLastPathComponent();
        Class<?> nodeType = selectedNode.getClass();

        if (nodeType.equals(TreeClassInfoModel.class)) {
            TreeClassInfoModel treeNode = (TreeClassInfoModel) selectedNode;

            if (informationPanel != null) {
                infoPanel.remove(informationPanel.getContent());
            }

            informationPanel = new SingleClassInfoWindow(project, insidiousService, treeNode);
            informationPanel.addEventHistoryLoadRequestListener(new LoadEventHistoryListener() {

                @Override
                public void loadEventHistory(long objectId) {

                    if (eventViewer != null) {
                        JPanel content = eventViewer.getContent();
                        eventViewer = null;
                        eventViewerPanel.remove(content);
                    }

                    ApplicationManager.getApplication().runReadAction(new Runnable() {
                        @Override
                        public void run() {
                            try {

                                ProgressManager.getInstance().run(new Task.WithResult<EventLogWindow, Exception>(
                                        project, "Unlogged", true
                                ) {
                                    @Override
                                    protected EventLogWindow compute(@NotNull ProgressIndicator indicator) throws Exception {
                                        eventViewer = new EventLogWindow(insidiousService);
                                        return eventViewer;
                                    }
                                });

                                eventViewerPanel.add(eventViewer.getContent(), constraints);

                            } catch (Exception ex) {
                                ex.printStackTrace();
                                InsidiousNotification.notifyMessage(
                                        "Failed to load object history: " + ex.getMessage(), NotificationType.ERROR);
                            }

                        }
                    });


                }
            });

            resultPanel.setDividerLocation(0.30d);

            GridConstraints constraints = new GridConstraints();
            constraints.setFill(GridConstraints.FILL_BOTH);

            infoPanel.add(informationPanel.getContent(), constraints);

        } else if (nodeType.equals(DefaultMutableTreeNode.class)) {
            try {
                treeModel.refreshSessionList();
            } catch (APICallException | IOException e) {
                InsidiousNotification.notifyMessage("Failed to refresh session list: "
                        + e.getMessage(), NotificationType.ERROR);
                throw new RuntimeException(e);
            }
        }

    }
}
