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
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
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
    private JButton copyVMParameterButton;
    private JProgressBar candidateLoadProgressbar;
    private TestCaseService testCaseService;
    private SessionInstance sessionInstance;
    private TestCandidateMethodAggregate selectedTestCandidateAggregate;


    public LiveViewWindow(Project project, InsidiousService insidiousService) {

        this.project = project;
        this.insidiousService = insidiousService;

        this.selectSession.addActionListener(selectSessionActionListener());

        cellRenderer = new VideobugTreeCellRenderer();
        mainTree.setCellRenderer(cellRenderer);
        TreeUtil.installActions(mainTree);
        mainTree.addTreeSelectionListener(this);
        copyVMParameterButton.addActionListener(e -> copyVMParameter());
        try {
            loadSession();
        } catch (Exception ex) {
            InsidiousNotification.notifyMessage("Failed to load session - " + ex.getMessage(), NotificationType.ERROR);
        }
        Color teal = new Color(1, 204, 245);
        setDividerColor(teal, splitPanel);
//        Color gray = Color.GRAY;
//        setDividerColor(gray,candidateInfoSplitPane);
//        setDividerColor(gray,inputOutputParent);
    }

    public static String[] splitByLength(String str, int size) {
        return (size < 1 || str == null) ? null : str.split("(?<=\\G.{" + size + "})");
    }

    private void copyVMParameter() {
        String vmParamString = insidiousService.getJavaAgentString();
        insidiousService.copyToClipboard(vmParamString);

    }

    @NotNull
    private ActionListener selectSessionActionListener() {
        return e -> {
            try {
                loadSession();
//                bgTask();
            } catch (Exception ex) {
                InsidiousNotification.notifyMessage("Failed to load session - " + ex.getMessage(),
                        NotificationType.ERROR);
            } finally {
                candidateListPanel.removeAll();
                candidateListPanel.revalidate();
                candidateListPanel.repaint();
            }
        };
    }

    public void loadSession() throws Exception {
        Task.Backgroundable task =
                new Task.Backgroundable(project, "Unlogged, Inc.", true) {


                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        checkProgressIndicator("Loading session", null);
                        insidiousService.getClient()
                                .getProjectSessions(new GetProjectSessionsCallback() {
                                    @Override
                                    public void error(String message) {
                                        InsidiousNotification.notifyMessage(
                                                "Failed to list sessions - " + message, NotificationType.ERROR);
                                    }

                                    @Override
                                    public void success(List<ExecutionSession> executionSessionList) {
                                        try {
                                            if (executionSessionList.size() == 0) {
                                                copyVMParameterButton.setVisible(true);
                                                String javaAgentVMString = insidiousService.getJavaAgentString();
                                                String[] parts = splitByLength(javaAgentVMString, 160);
                                                assert parts != null;
                                                StringBuilder text = new StringBuilder(
                                                        "<html>No session found. Run your application with unlogged agent to record a new session.<br /><br />" +
                                                                "Unlogged java agent jar is downloaded at: <br />" + insidiousService.getVideoBugAgentPath() + "<br /><br />" +
                                                                "Use the following VM parameter to start your application with unlogged java agent:<br />");
                                                for (String part : parts) {
                                                    text.append(part)
                                                            .append("<br />");
                                                }
                                                text.append("</html>");

                                                headingText.setText(text.toString());
                                                mainTree.setModel(new DefaultTreeModel(
                                                        new DefaultMutableTreeNode("No session")));
                                                return;
                                            } else {
                                                headingText.setText(
                                                        "Select a class and method to start generating test case for it.");
                                                copyVMParameterButton.setVisible(false);
                                            }
                                            ExecutionSession executionSession = executionSessionList.get(0);

                                            if (sessionInstance != null) {
//                                        if (sessionInstance.getExecutionSession().getSessionId().equals(executionSession.getSessionId())) {
//                                            testCaseService.processLogFiles();
//                                            treeModel = new LiveViewTestCandidateListTree(
//                                                    project, insidiousService.getClient().getSessionInstance());
//                                            mainTree.setModel(treeModel);
//                                            return;
//                                        }
                                                sessionInstance.close();
                                            }
//                                    sessionInstance = null;
                                            sessionInstance = new SessionInstance(executionSession, project);
                                            insidiousService.getClient()
                                                    .setSessionInstance(sessionInstance);
                                            testCaseService = new TestCaseService(sessionInstance);

                                            testCaseService.processLogFiles();
                                            treeModel = new LiveViewTestCandidateListTree(
                                                    project, insidiousService.getClient()
                                                    .getSessionInstance());
                                            mainTree.setModel(treeModel);

                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                            InsidiousNotification.notifyMessage(
                                                    "Failed to set sessions - " + ex.getMessage()
                                                            + "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.",
                                                    NotificationType.ERROR);
                                            try {
                                                JSONObject eventProperties = new JSONObject();
                                                eventProperties.put("exception", ex.getMessage());
                                                UsageInsightTracker.getInstance()
                                                        .RecordEvent("ScanFailed", eventProperties);
                                            } catch (Exception e) {
                                                logger.error("Failed to send ScanFailed event to amplitude");
                                            }
                                        }
                                    }
                                });
                    }
                };

        ProgressManager.getInstance()
                .run(task);

    }

    public JComponent getContent() {
        return mainPanel;
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        logger.warn("value selection event - " + e.getPath() + " - " + e.getPath()
                .getLastPathComponent());
        Object selectedNode = e.getPath()
                .getLastPathComponent();
        if (selectedNode.getClass()
                .equals(TestCandidateMethodAggregate.class)) {
            selectedTestCandidateAggregate = (TestCandidateMethodAggregate) selectedNode;
            loadTestCandidateConfigView(selectedTestCandidateAggregate);

        }
    }

    private void loadTestCandidateConfigView(TestCandidateMethodAggregate methodNode) {
        List<TestCandidateMetadata> testCandidateMetadataList =
                sessionInstance.getTestCandidatesForMethod(
                        methodNode.getClassName(), methodNode.getMethodName(), false);

        this.candidateListPanel.removeAll();
        this.candidateListPanel.setLayout(new GridLayout(1, 1));
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(1);
        CandidateInformationWindow candidateInformationWindow = new CandidateInformationWindow(testCandidateMetadataList, testCaseService, this, sessionInstance);
        candidateListPanel.add(candidateInformationWindow.getMainPanel(), constraints);
        headingText.setText("Test Candidates for " + methodNode.getMethodName());
        this.candidateListPanel.revalidate();
    }

    private void setLoadingState(boolean status) {
        candidateLoadProgressbar.setVisible(status);
    }

    @Override
    public void onSelect(TestCandidateMetadata testCandidateMetadata) {
        try {
            setLoadingState(true);
            TestCandidateCustomizeView testCandidateView = new TestCandidateCustomizeView(
                    testCandidateMetadata, sessionInstance, this);
            this.candidateListPanel.removeAll();
            this.candidateListPanel.setLayout(new GridLayout(1, 1));
            GridConstraints constraints = new GridConstraints();
            constraints.setRow(1);
            candidateListPanel.add(testCandidateView.getContentPanel(), constraints);
            this.candidateListPanel.revalidate();
            setLoadingState(false);
        } catch (Exception e) {
            e.printStackTrace();
            setLoadingState(false);
        }
    }

    private void checkProgressIndicator(String text1, String text2) {
        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            if (ProgressIndicatorProvider.getGlobalProgressIndicator()
                    .isCanceled()) {
                throw new ProcessCanceledException();
            }
            if (text2 != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator()
                        .setText2(text2);
            }
            if (text1 != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator()
                        .setText(text1);
            }
        }
    }


    @Override
    public void generateTestCase(TestCaseGenerationConfiguration generationConfiguration) throws IOException {

        @NotNull TestCaseUnit testCaseUnit = testCaseService.buildTestCaseUnit(generationConfiguration);

        TestSuite testSuite = new TestSuite(List.of(testCaseUnit));
        insidiousService.ensureTestUtilClass();
        insidiousService.saveTestSuite(testSuite);

        InsidiousNotification.notifyMessage("Testcase generated for " + testCaseUnit.getTestMethodName() + "()",
                NotificationType.INFORMATION);

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
    public void loadInputOutputInformation(TestCandidateMetadata metadata) {}

    @Override
    public void cancel() {
        loadTestCandidateConfigView(selectedTestCandidateAggregate);
    }

    private void setDividerColor(Color color, JSplitPane splitPanel) {
        splitPanel.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    public void setBorder(Border b) {
                    }

                    @Override
                    public void paint(Graphics g) {
                        g.setColor(color);
                        g.fillRect(0, 0, getSize().width, getSize().height);
                        super.paint(g);
                    }
                };
            }
        });
    }
}
