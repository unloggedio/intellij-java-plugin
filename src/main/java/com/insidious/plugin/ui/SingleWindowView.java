package com.insidious.plugin.ui;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.ui.tree.TreeUtil;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.ExpandVetoException;

public class SingleWindowView implements TreeExpansionListener, TreeWillExpandListener, TreeSelectionListener {
    public static final String CLASSES_LABEL = "Classes";
    private final Project project;
    private final InsidiousService insidiousService;
    private final Logger logger = LoggerUtil.getInstance(SingleWindowView.class);
    private final VideobugTreeCellRenderer cellRenderer;
    private JTree mainTree;
    private JButton refreshButton;
    private JPanel mainPanel;
    private JPanel filterPanel;
    private JSplitPane resultPanel;
    private JScrollPane treePanel;
    private JPanel infoPanel;
    private JTextField textField1;
    private JButton button1;
    private SingleClassInfoWindow informationPanel;
    private JScrollPane detailedView;

    public SingleWindowView(Project project, InsidiousService insidiousService) {

        this.project = project;


//        refreshButton.addActionListener(e -> {
//            try {
//                refresh();
//            }catch (Throwable e1) {
//                e1.printStackTrace();
//            }
//        });
        this.insidiousService = insidiousService;
        mainTree.addTreeExpansionListener(SingleWindowView.this);
        mainTree.addTreeWillExpandListener(SingleWindowView.this);
        mainTree.addTreeSelectionListener(SingleWindowView.this);

//        mainPanel.remove(detailedView);
        mainTree.setModel(new VideobugTreeModel(insidiousService));


        cellRenderer = new VideobugTreeCellRenderer(insidiousService);

        mainTree.setCellRenderer(cellRenderer);
        resultPanel.setDividerLocation(0.99d);
        TreeUtil.installActions(mainTree);


        refresh();

    }

    public void refresh() {

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

            resultPanel.setDividerLocation(0.50d);

            GridConstraints constraints = new GridConstraints();
            constraints.setFill(GridConstraints.FILL_BOTH);

            infoPanel.add(informationPanel.getContent(), constraints);

        }

    }
}
