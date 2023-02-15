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
import com.insidious.plugin.ui.Components.LiveViewInfoBanner;
import com.insidious.plugin.upload.minio.ReportIssueForm;
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
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.text.SimpleDateFormat;

public class LiveViewWindow implements TreeSelectionListener,
        TestSelectionListener, TestGenerateActionListener, NewTestCandidateIdentifiedListener,
        RefreshButtonStateManager {
    private static final Logger logger = LoggerUtil.getInstance(LiveViewWindow.class);
    static boolean isLoading = false;
    private final Project project;
    private final VideobugTreeCellRenderer cellRenderer;
    private final ActionListener pauseActionListener;
    private final ActionListener resumeActionListener;
    Icon refreshDefaultIcon;
    private LiveViewTestCandidateListTree treeModel;
    private JButton processLogsSwitch;
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
    private JProgressBar progressBar1;
    private JButton pauseProcessingButton;
    private JButton reportIssueButton;
    private JPanel sessionControls;
    private JLabel statusTextHeading;
    private TestCaseService testCaseService;
    private SessionInstance sessionInstance;
    private TestCandidateMethodAggregate selectedTestCandidateAggregate;

    private JFrame reportIssueForm;

    public LiveViewWindow(Project project, InsidiousService insidiousService) {

        this.project = project;

        this.processLogsSwitch.addActionListener(selectSessionActionListener());
        pauseActionListener = pauseCheckingForNewLogs();
        resumeActionListener = resumeCheckingForNewLogs();
        topControlPanel.remove(pauseProcessingButton);
        topControlPanel.remove(progressBar1);
        mainTree.setModel(new DefaultTreeModel(
                new DefaultMutableTreeNode(new StringBuilder("Loading Packages"))));
        reportIssueForm = new ReportIssueForm(project);
        reportIssueForm.setVisible(false);

        reportIssueButton.addActionListener(openReportIssueForm());
        reportIssueButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cellRenderer = new VideobugTreeCellRenderer();
        mainTree.setCellRenderer(cellRenderer);
        TreeUtil.installActions(mainTree);
        mainTree.addTreeSelectionListener(this);
        copyVMParameterButton.addActionListener(e -> copyVMParameter());
        refreshDefaultIcon = this.processLogsSwitch.getIcon();
        loadInfoBanner();
        try {
            loadSession();
        } catch (Exception ex) {
            InsidiousNotification.notifyMessage("Failed to load session - " + ex.getMessage(), NotificationType.ERROR);
        }
        UIUtils.setDividerColorForSplitPane(splitPanel, UIUtils.teal);
        this.processLogsSwitch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static String[] splitByLength(String str, int size) {
        return (size < 1 || str == null) ? null : str.split("(?<=\\G.{" + size + "})");
    }


    ActionListener openReportIssueForm() {
        return e -> {
            reportIssueForm.setVisible(true);
            checkProgressIndicator("Uploading session logs with report", null);
        };
    }


    private void copyVMParameter() {
        InsidiousService insidiousService = project.getService(InsidiousService.class);
        String vmParamString = insidiousService.getJavaAgentString();
        insidiousService.copyToClipboard(vmParamString);
        InsidiousNotification.notifyMessage("VM options copied to clipboard.",
                NotificationType.INFORMATION);
    }

    private void updateRefreshButtonState() {
        if (isLoading) {
            setState_Processing();
        }
        else
        {
            processLogsSwitch.setText("Updating");
        }
        mainPanel.validate();
        mainPanel.repaint();
    }

    @NotNull
    private ActionListener selectSessionActionListener() {
        return e -> {
            try {
                if (!isLoading) {
                    loadSession();
                }
            } catch (Exception ex) {
                InsidiousNotification.notifyMessage("Failed to load session - " + ex.getMessage(),
                        NotificationType.ERROR);
            } finally {
                loadInfoBanner();
            }
        };
    }

    @NotNull
    private ActionListener pauseCheckingForNewLogs() {
        return e -> setPauseScanningState();
    }

    private void setPauseScanningState() {
        if (testCaseService != null) {
            testCaseService.setPauseCheckingForNewLogs(true);
        }
        pauseProcessingButton.setText("Resume processing logs");
        isLoading = false;
        updateRefreshButtonState();
        pauseProcessingButton.removeActionListener(pauseActionListener);
        pauseProcessingButton.addActionListener(resumeActionListener);
    }

    @NotNull
    private ActionListener resumeCheckingForNewLogs() {
        return e -> setResumedScanningState();
    }

    private void setResumedScanningState() {
        testCaseService.setPauseCheckingForNewLogs(false);
        pauseProcessingButton.setText("Pause processing logs");
        isLoading = true;
        updateRefreshButtonState();
        pauseProcessingButton.removeActionListener(resumeActionListener);
        pauseProcessingButton.addActionListener(pauseActionListener);
    }

    public void setTreeStateToLoading() {
        if (!(this.mainTree.getModel() instanceof LiveViewTestCandidateListTree)) {
            mainTree.setModel(new DefaultTreeModel(
                    new DefaultMutableTreeNode(new StringBuilder("Loading Packages"))));
        }
    }

    public void updateTreeStateOnScanFailure() {
        if (!(this.mainTree.getModel() instanceof LiveViewTestCandidateListTree)) {
            mainTree.setModel(new DefaultTreeModel(
                    new DefaultMutableTreeNode(new StringBuilder("Failed to set session"))));
        }
    }

    public void loadSession() {
        isLoading = true;
        updateRefreshButtonState();
        Task.Backgroundable task =
                new Task.Backgroundable(project, "Unlogged, Inc.", true) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        checkProgressIndicator("Loading session", null);
                        final InsidiousService insidiousService = project.getService(InsidiousService.class);
                        insidiousService.getClient()
                                .getProjectSessions(new GetProjectSessionsCallback() {
                                    @Override
                                    public void error(String message) {
                                        isLoading = false;
                                        updateRefreshButtonState();
                                        updateTreeStateOnScanFailure();
                                        InsidiousNotification.notifyMessage(
                                                "Failed to list sessions - " + message, NotificationType.ERROR);
                                    }

                                    @Override
                                    public void success(List<ExecutionSession> executionSessionList) {
                                        try {
                                            if (executionSessionList.size() == 0) {
                                                //copyVMParameterButton.setVisible(true);
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
                                                isLoading = false;
                                                updateRefreshButtonState();
                                                return;
                                            } else {
//                                                headingText.setText(
//                                                        "Select a class and method to start generating test case for it.");
                                                copyVMParameterButton.setVisible(false);
                                            }
                                            ExecutionSession executionSession = executionSessionList.get(0);

                                            //has session instance, check if it's the same as the last one
                                            //if yes, refresh

                                            //else
                                            //process logs
                                            boolean startScan_Full=true;
                                            if (sessionInstance != null) {
                                                if(!Objects.equals(
                                                        sessionInstance.getExecutionSession()
                                                                .getSessionId(), executionSession.getSessionId()))
                                                {
                                                    startScan_Full=true;
                                                    sessionInstance.close();
                                                }
                                                else
                                                {
                                                    //resume scan
                                                    startScan_Full = false;
                                                    testCaseService.processLogFiles();
                                                    treeModel = new LiveViewTestCandidateListTree(
                                                            project, insidiousService.getClient()
                                                            .getSessionInstance());
                                                    mainTree.setModel(treeModel);
                                                }
                                            }
                                            if(startScan_Full) {
                                                sessionInstance = new SessionInstance(executionSession, project);
                                                insidiousService.getClient()
                                                        .setSessionInstance(sessionInstance);
                                                testCaseService = new TestCaseService(sessionInstance);
                                                sessionInstance.setTestCandidateListener(LiveViewWindow.this);
                                                testCaseService.processLogFiles();
                                                testCaseService.setRefreshButtonStateManager(getLiveViewReference());
                                                testCaseService.startRun();
                                                treeModel = new LiveViewTestCandidateListTree(
                                                        project, insidiousService.getClient()
                                                        .getSessionInstance());
                                                mainTree.setModel(treeModel);
                                            }
                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                            setPauseScanningState();
                                            InsidiousNotification.notifyMessage(
                                                    "Failed to set sessions - " + ex.getMessage()
                                                            + "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.",
                                                    NotificationType.ERROR);
                                            updateTreeStateOnScanFailure();
                                            try {
                                                JSONObject eventProperties = new JSONObject();
                                                eventProperties.put("exception", ex.getMessage());
                                                UsageInsightTracker.getInstance()
                                                        .RecordEvent("ScanFailed", eventProperties);
                                            } catch (Exception e) {
                                                logger.error("Failed to send ScanFailed event to amplitude");
                                            }
                                        } finally {
                                            isLoading = false;
                                            updateRefreshButtonState();
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
        CandidateInformationWindow candidateInformationWindow = new CandidateInformationWindow(
                testCandidateMetadataList, testCaseService, this, sessionInstance);
        candidateListPanel.add(candidateInformationWindow.getMainPanel(), constraints);
        headingText.setText("Test Candidates for " + methodNode.getMethodName() + " (most recent first)");
        this.candidateListPanel.revalidate();
    }

    @Override
    public void onSelect(TestCandidateMetadata testCandidateMetadata) {
        try {
            UsageInsightTracker.getInstance().
                    RecordEvent("CandidateSelected", null);
            TestCandidateCustomizeView testCandidateView = new TestCandidateCustomizeView(
                    testCandidateMetadata, sessionInstance, this);
            this.candidateListPanel.removeAll();
            this.candidateListPanel.setLayout(new GridLayout(1, 1));
            GridConstraints constraints = new GridConstraints();
            constraints.setRow(1);
            candidateListPanel.add(testCandidateView.getContentPanel(), constraints);
            this.candidateListPanel.revalidate();
        } catch (Exception e) {
            e.printStackTrace();
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
    public void generateTestCase(TestCaseGenerationConfiguration generationConfiguration) throws Exception {

        @NotNull TestCaseUnit testCaseUnit = testCaseService.buildTestCaseUnit(generationConfiguration);

        List<TestCaseUnit> testCaseUnit1 = new ArrayList<>();
        testCaseUnit1.add(testCaseUnit);
        TestSuite testSuite = new TestSuite(testCaseUnit1);
        project.getService(InsidiousService.class)
                .saveTestSuite(testSuite);

        InsidiousNotification.notifyMessage("Testcase generated for " + testCaseUnit.getTestMethodName() + "()",
                NotificationType.INFORMATION);

    }

    @Override
    public void loadInputOutputInformation(TestCandidateMetadata metadata) {
    }

    @Override
    public void cancel() {
        loadTestCandidateConfigView(selectedTestCandidateAggregate);
    }

    @Override
    public void onNewTestCandidateIdentified(int completedCount, int totalCount) {
        try {
            treeModel = new LiveViewTestCandidateListTree(
                    project, project.getService(InsidiousService.class).getClient().getSessionInstance());
            mainTree.setModel(treeModel);
            mainTree.validate();
            mainTree.repaint();
            if (completedCount == 0) {
                progressBar1.setMaximum(1);
                progressBar1.setValue(1);
            } else {
                progressBar1.setMaximum(totalCount);
                progressBar1.setValue(completedCount);
            }
            mainPanel.validate();
            mainPanel.repaint();
        } catch (Exception e) {
            logger.error("failed to query new candidates", e);
            throw new RuntimeException(e);
        }
    }

    public void loadInfoBanner()
    {
        this.headingText.setText("");
        LiveViewInfoBanner banner = new LiveViewInfoBanner();
        this.candidateListPanel.removeAll();
        this.candidateListPanel.setLayout(new GridLayout(1, 1));
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(1);
        candidateListPanel.add(banner.getComponent(), constraints);
        this.candidateListPanel.revalidate();
    }

    @Override
    public void setState_NewLogs(Date lastScannedTimeStamp) {
        processLogsSwitch.setBackground(UIUtils.green);
        processLogsSwitch.setText("New logs available (click to process)");
        processLogsSwitch.setIcon(UIUtils.NEW_LOGS_TO_PROCESS_ICON);
        statusTextHeading.setText("Last Scan completed at : "+
                getFormattedDate(sessionInstance.getLastScannedTimeStamp()));
    }

    @Override
    public void setState_NoNewLogs(Date lastScannedTimeStamp) {
        processLogsSwitch.setBackground(UIUtils.NeutralGrey);
        processLogsSwitch.setText("No New logs available (use application to create logs)");
        processLogsSwitch.setIcon(UIUtils.NO_NEW_LOGS_TO_PROCESS_ICON);
        statusTextHeading.setText("Last Scan completed at : "+
                getFormattedDate(sessionInstance.getLastScannedTimeStamp()));
    }

    @Override
    public void setState_NewSession() {
        processLogsSwitch.setBackground(UIUtils.green);
        processLogsSwitch.setText("New Session available (click to process)");
        processLogsSwitch.setIcon(UIUtils.NEW_LOGS_TO_PROCESS_ICON);
    }

    @Override
    public void setState_Processing() {
        UIUtils.setGifIconForButton(this.processLogsSwitch, "loading-def.gif", refreshDefaultIcon);
        processLogsSwitch.setBackground(UIUtils.teal);
        processLogsSwitch.setText("Processing Logs");
        statusTextHeading.setText("");
    }

    @Override
    public boolean isProcessing() {
        return isLoading;
    }

    private LiveViewWindow getLiveViewReference()
    {
        return this;
    }

    private String getFormattedDate(Date date)
    {
        if(date==null)
        {
            return "";
        }
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss | dd MMM, yyyy");
        return formatter.format(date);
    }
}
