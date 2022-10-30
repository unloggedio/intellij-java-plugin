package com.insidious.plugin.ui;

import com.insidious.plugin.callbacks.GetProjectSessionsCallback;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.client.TestCandidateMethodAggregate;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.TestCaseService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.TestCaseUnit;
import com.insidious.plugin.pojo.TestSuite;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;

public class LiveViewWindow implements TreeSelectionListener,
        TestSelectionListener, TestGenerateActionListener {
    private static final Logger logger = LoggerUtil.getInstance(LiveViewWindow.class);
    private final Project project;
    private final InsidiousService insidiousService;
    private final VideobugTreeCellRenderer cellRenderer;
    private LiveViewTestCandidateListTree treeModel;
    private JButton selectSession;
    private JPanel bottomPanel;
    private JTree mainTree;
    private JPanel mainPanel;
    private JSplitPane splitPanel;
    private JScrollPane mainTreeScrollPanel;
    private JPanel topControlPanel;
    private JPanel candidateListPanel;
    private TestCaseService testCaseService;
    private SessionInstance sessionInstance;
    private TestCandidateMethodAggregate selectedTestCandidateAggregate;


    public LiveViewWindow(Project project, InsidiousService insidiousService) {

        this.project = project;
        this.insidiousService = insidiousService;

        this.selectSession.addActionListener(selectSessionActionListener());

        cellRenderer = new VideobugTreeCellRenderer(insidiousService);
        mainTree.setCellRenderer(cellRenderer);
        TreeUtil.installActions(mainTree);
        mainTree.addTreeSelectionListener(this);
        try {
            loadSession();
        } catch (Exception ex) {
            InsidiousNotification.notifyMessage("Failed to load session - " + ex.getMessage(), NotificationType.ERROR);
        }
    }

    @NotNull
    private ActionListener selectSessionActionListener() {
        return e -> {
            try {
                loadSession();
            } catch (Exception ex) {
                InsidiousNotification.notifyMessage("Failed to load session - " + ex.getMessage(), NotificationType.ERROR);
            }
        };
    }

    public void loadSession() throws Exception {
        treeModel = ProgressManager.getInstance().run(
                new Task.WithResult<LiveViewTestCandidateListTree, Exception>(project, "Unlogged", true) {
                    @Override
                    protected LiveViewTestCandidateListTree compute(@NotNull ProgressIndicator indicator) throws Exception {
                        insidiousService.getClient().getProjectSessions(new GetProjectSessionsCallback() {
                            @Override
                            public void error(String message) {
                                InsidiousNotification.notifyMessage("Failed to list sessions - " + message, NotificationType.ERROR);
                            }

                            @Override
                            public void success(List<ExecutionSession> executionSessionList) {
                                try {
                                    ExecutionSession executionSession = executionSessionList.get(0);
                                    sessionInstance = new SessionInstance(executionSession);
                                    insidiousService.getClient().setSessionInstance(sessionInstance);
                                    testCaseService = new TestCaseService(sessionInstance);

                                    testCaseService.processLogFiles();
                                    treeModel = new LiveViewTestCandidateListTree(insidiousService.getClient().getSessionInstance());
                                    mainTree.setModel(treeModel);

                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    InsidiousNotification.notifyMessage("Failed to set sessions - " + ex.getMessage(), NotificationType.ERROR);
                                }
                            }
                        });
                        return null;
                    }
                });

    }

    public JComponent getContent() {
        return mainPanel;
    }


    @Override
    public void valueChanged(TreeSelectionEvent e) {
        logger.warn("value selection event - " + e.getPath() + " - " + e.getPath().getLastPathComponent());
        Object selectedNode = e.getPath().getLastPathComponent();
        if (selectedNode.getClass().equals(TestCandidateMethodAggregate.class)) {
            selectedTestCandidateAggregate = (TestCandidateMethodAggregate) selectedNode;
            loadTestCandidateConfigView(selectedTestCandidateAggregate);

        }
    }

    private void loadTestCandidateConfigView(TestCandidateMethodAggregate methodNode) {
        List<TestCandidateMetadata> testCandidateMetadataList =
                sessionInstance.getTestCandidatesForMethod(methodNode.getClassName(), methodNode.getMethodName(), false);

        this.candidateListPanel.removeAll();
        JPanel gridPanel = new JPanel(new GridLayout(testCandidateMetadataList.size(), 1));
        //this.candidateListPanel.setLayout(new GridLayout(testCandidateMetadataList.size(), 1));
        for (int i = 0; i < testCandidateMetadataList.size(); i++) {
            TestCandidateMetadata testCandidateMetadata = testCandidateMetadataList.get(i);
            GridConstraints constraints = new GridConstraints();
            constraints.setRow(i);
            TestCandidateMetadataView testCandidatePreviewPanel = new TestCandidateMetadataView(
                    testCandidateMetadata, testCaseService, this);
            testCandidatePreviewPanel.setCandidateNumberIndex((i+1));
            Component contentPanel = testCandidatePreviewPanel.getContentPanel();
            gridPanel.add(contentPanel, constraints);
        }

        JScrollPane scrollPane = new JScrollPane(gridPanel);
        candidateListPanel.setPreferredSize(scrollPane.getSize());
        candidateListPanel.add(scrollPane,BorderLayout.CENTER);
        this.candidateListPanel.revalidate();
    }

    @Override
    public void onSelect(TestCandidateMetadata testCandidateMetadata) {

        TestCandidateCustomizeView testCandidateView = new TestCandidateCustomizeView(
                testCandidateMetadata, sessionInstance, this);
        this.candidateListPanel.removeAll();
        this.candidateListPanel.setLayout(new GridLayout(1, 1));
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(1);

        candidateListPanel.add(testCandidateView.getContentPanel(), constraints);
        this.candidateListPanel.revalidate();


    }

    @Override
    public void generateTestCase(
            TestCaseGenerationConfiguration generationConfiguration
    ) {
        @NotNull TestCaseUnit testCaseUnit = testCaseService.getTestCaseUnit(generationConfiguration);
        TestSuite testSuite = new TestSuite(List.of(testCaseUnit));
        insidiousService.saveTestSuite(testSuite);
    }

    @Override
    public void cancel() {
        loadTestCandidateConfigView(selectedTestCandidateAggregate);
    }
}
