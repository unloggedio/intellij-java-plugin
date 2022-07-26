package com.insidious.plugin.ui;

import com.insidious.common.weaver.ClassInfo;
import com.insidious.common.weaver.MethodInfo;
import com.insidious.plugin.callbacks.ClientCallBack;
import com.insidious.plugin.callbacks.GetProjectSessionsCallback;
import com.insidious.plugin.client.pojo.ExceptionResponse;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.pojo.ClassWeaveInfo;
import com.insidious.plugin.pojo.ObjectsWithTypeInfo;
import com.insidious.plugin.pojo.SearchQuery;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.uiDesigner.core.GridConstraints;


import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH;

public class SingleWindowView implements TreeExpansionListener, TreeWillExpandListener, TreeSelectionListener {
    public static final String CLASSES_LABEL = "Classes";
    private final Project project;
    private final InsidiousService insidiousService;
    private JTree mainTree;
    private JButton refreshButton;
    private JPanel mainPanel;
    private JScrollPane treePanel;
    private JPanel controlPanel;
    private JScrollPane detailedView;
    private final Logger logger = LoggerUtil.getInstance(SingleWindowView.class);

    public SingleWindowView(Project project, InsidiousService insidiousService) {

        this.project = project;


        refreshButton.addActionListener(e -> refresh());
        this.insidiousService = insidiousService;
        mainTree.addTreeExpansionListener(SingleWindowView.this);
        mainTree.addTreeWillExpandListener(SingleWindowView.this);
        mainTree.addTreeSelectionListener(SingleWindowView.this);

        mainPanel.remove(detailedView);
        mainTree.setModel(new VideobugTreeModel(insidiousService));
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
            insidiousService.getClient().getObjectsByType(
                    SearchQuery.ByType(List.of(treeNode.getClassName())),
                    treeNode.getSessionId(), new ClientCallBack<ObjectsWithTypeInfo>() {
                        @Override
                        public void error(ExceptionResponse errorResponse) {

                        }

                        @Override
                        public void success(Collection<ObjectsWithTypeInfo> tracePoints) {

                        }

                        @Override
                        public void completed() {
                            mainPanel.add(detailedView);
                        }
                    }
            );
        }

    }
}
