package com.insidious.plugin.ui;

import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.client.TestCandidateMethodAggregate;
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
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LiveViewWindow implements TreeSelectionListener,
        TestSelectionListener, TestGenerateActionListener {
    private static final Logger logger = LoggerUtil.getInstance(LiveViewWindow.class);
    static boolean isLoading = false;
    private final Project project;
    private final VideobugTreeCellRenderer cellRenderer;
    private LiveViewTestCandidateListTree treeModel;
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
    private JLabel statusTextHeading;
    private TestCaseService testCaseService;
    private SessionInstance sessionInstance;
    private TestCandidateMethodAggregate selectedTestCandidateAggregate;

    private JFrame reportIssueForm;

    public LiveViewWindow(Project project) {

        this.project = project;

        topControlPanel.remove(pauseProcessingButton);
        topControlPanel.remove(progressBar1);
        mainTree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode(new StringBuilder("Loading Packages"))));
        reportIssueForm = new ReportIssueForm(project);
        reportIssueForm.setVisible(false);

        reportIssueButton.addActionListener(openReportIssueForm());
        reportIssueButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cellRenderer = new VideobugTreeCellRenderer();
        mainTree.setCellRenderer(cellRenderer);
        TreeUtil.installActions(mainTree);
        mainTree.addTreeSelectionListener(this);
        copyVMParameterButton.addActionListener(e -> copyVMParameter());
        loadInfoBanner();

        UIUtils.setDividerColorForSplitPane(splitPanel, UIUtils.teal);
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
                testCaseService.getTestCandidatesForMethod(
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


    public void loadInfoBanner() {
        this.headingText.setText("");
        LiveViewInfoBanner banner = new LiveViewInfoBanner();
        this.candidateListPanel.removeAll();
        this.candidateListPanel.setLayout(new GridLayout(1, 1));
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(1);
        candidateListPanel.add(banner.getComponent(), constraints);
        this.candidateListPanel.revalidate();
    }

    private LiveViewWindow getLiveViewReference() {
        return this;
    }

    private String getFormattedDate(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss | dd MMM, yyyy");
        return formatter.format(date);
    }
}
