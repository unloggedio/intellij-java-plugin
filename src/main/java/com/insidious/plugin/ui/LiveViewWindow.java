package com.insidious.plugin.ui;

import com.insidious.plugin.callbacks.GetProjectSessionsCallback;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.TestCaseService;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.List;

public class LiveViewWindow {
    private final Project project;
    private final InsidiousService insidiousService;
    private final VideobugTreeCellRenderer cellRenderer;
    private NewVideobugTreeModel treeModel;
    private JButton selectSession;
    private JPanel bottomPanel;
    private JPanel leftSplitContainer;
    private JTree mainTree;
    private JPanel mainPanel;


    public LiveViewWindow(Project project, InsidiousService insidiousService) {
        this.project = project;
        this.insidiousService = insidiousService;

        this.selectSession.addActionListener(selectSessionActionListener());

        cellRenderer = new VideobugTreeCellRenderer(insidiousService);
        mainTree.setCellRenderer(cellRenderer);
        TreeUtil.installActions(mainTree);
        loadSession();

    }

    @NotNull
    private ActionListener selectSessionActionListener() {
        return e -> {
            loadSession();
        };
    }

    public void loadSession() {
        insidiousService.getClient().getProjectSessions(new GetProjectSessionsCallback() {
            @Override
            public void error(String message) {
                InsidiousNotification.notifyMessage("Failed to list sessions - " + message, NotificationType.ERROR);
            }

            @Override
            public void success(List<ExecutionSession> executionSessionList) {
                try {
                    ExecutionSession executionSession = executionSessionList.get(0);
                    insidiousService.getClient().setSessionInstance(executionSession);
                    SessionInstance sessionInstance = insidiousService.getClient().getSessionInstance();
                    TestCaseService testCaseService = new TestCaseService(insidiousService.getClient());
                    treeModel = new NewVideobugTreeModel(testCaseService);
                    mainTree.setModel(treeModel);

                } catch (Exception ex) {
                    ex.printStackTrace();
                    InsidiousNotification.notifyMessage("Failed to set sessions - " + ex.getMessage(), NotificationType.ERROR);
                }
            }
        });
    }

    public JComponent getContent() {
        return mainPanel;
    }


}
