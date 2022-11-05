package com.insidious.plugin.ui;

import com.insidious.plugin.callbacks.GetProjectSessionsCallback;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.client.TestCandidateMethodAggregate;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.testcase.TestCaseService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.TestCaseUnit;
import com.insidious.plugin.pojo.TestSuite;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.Project;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;
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
    private JPanel candidateParentPanel;
    private JPanel headingPanel;
    private JLabel headingText;
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
        setDividerColor();
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
                                    treeModel = new LiveViewTestCandidateListTree(
                                            project, insidiousService.getClient().getSessionInstance());
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
                sessionInstance.getTestCandidatesForMethod(
                        methodNode.getClassName(), methodNode.getMethodName(), false);

        this.candidateListPanel.removeAll();
        int GridRows = 6;
        if (testCandidateMetadataList.size() > GridRows) {
            GridRows = testCandidateMetadataList.size();
        }
        GridLayout gridLayout = new GridLayout(GridRows,1);
        gridLayout.setVgap(8);
        JPanel gridPanel = new JPanel(gridLayout);
        for (int i = 0; i < testCandidateMetadataList.size(); i++) {
            TestCandidateMetadata testCandidateMetadata = testCandidateMetadataList.get(i);
            GridConstraints constraints = new GridConstraints();
            constraints.setRow(i);
            TestCandidateMetadataView testCandidatePreviewPanel = new TestCandidateMetadataView(
                    testCandidateMetadata, testCaseService, this, sessionInstance);
            testCandidatePreviewPanel.setCandidateNumberIndex((i + 1));
            Component contentPanel = testCandidatePreviewPanel.getContentPanel();
            gridPanel.add(contentPanel, constraints);
        }

        JScrollPane scrollPane = new JScrollPane(gridPanel);
        candidateListPanel.setPreferredSize(scrollPane.getSize());
        candidateListPanel.add(scrollPane, BorderLayout.CENTER);
        if (testCandidateMetadataList.size() <= 4) {
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        }
        headingText.setText("Test Candidates for "+methodNode.getMethodName());
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

    private void checkProgressIndicator(String text1, String text2) {
        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                throw new ProcessCanceledException();
            }
            if (text2 != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2(text2);
            }
            if (text1 != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText(text1);
            }
        }
    }


    @Override
    public void generateTestCase(TestCaseGenerationConfiguration generationConfiguration) throws IOException {

        @NotNull TestCaseUnit testCaseUnit = testCaseService.buildTestCaseUnit(generationConfiguration);

        TestSuite testSuite = new TestSuite(List.of(testCaseUnit));
        insidiousService.ensureTestUtilClass();
        insidiousService.saveTestSuite(testSuite);

        UsageInsightTracker.getInstance().RecordEvent("TestCaseGenerated",null);

//        try {
//            ProgressManager.getInstance().run(new Task.WithResult<Void, Exception>(project, "Unlogged", false) {
//                @Override
//                protected Void compute(@NotNull ProgressIndicator indicator) throws Exception {
//
//                    Parameter testSubject = generationConfiguration.getTestCandidateMetadataList().get(0).getTestSubject();
//                    checkProgressIndicator("Generating test case: " + testSubject.getType(),
//                            "With " + generationConfiguration.getTestCandidateMetadataList().size() + " candidates," +
//                                    " mocking " + generationConfiguration.getCallExpressionList().size());
//                    @NotNull TestCaseUnit testCaseUnit = testCaseService.buildTestCaseUnit(generationConfiguration);
//
//                    TestSuite testSuite = new TestSuite(List.of(testCaseUnit));
//                    insidiousService.ensureTestUtilClass();
//                    insidiousService.saveTestSuite(testSuite);
//                    return null;
//                }
//            });
//        } catch (Exception e) {
//            InsidiousNotification.notifyMessage(
//                    "Failed to generated test case - " + e.getMessage(), NotificationType.ERROR
//            );
//        }

    }

    @Override
    public void cancel() {
        loadTestCandidateConfigView(selectedTestCandidateAggregate);
    }

    private void setDividerColor()
    {
        splitPanel.setUI(new BasicSplitPaneUI()
        {
            @Override
            public BasicSplitPaneDivider createDefaultDivider()
            {
                return new BasicSplitPaneDivider(this)
                {
                    public void setBorder(Border b) {}

                    @Override
                    public void paint(Graphics g)
                    {
                        Color teal = new Color(1,204,245);
                        g.setColor(teal);
                        g.fillRect(0, 0, getSize().width, getSize().height);
                        super.paint(g);
                    }
                };
            }
        });
    }
}
