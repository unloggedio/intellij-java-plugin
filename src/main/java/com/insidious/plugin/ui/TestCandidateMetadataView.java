package com.insidious.plugin.ui;

import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.factory.testcase.TestCaseService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.MethodCallExpression;

import javax.swing.*;
import java.awt.*;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestCandidateMetadataView {
    private final TestCandidateMetadata testCandidateMetadata;
    private final TestCaseService testCaseService;
    private final TestSelectionListener candidateSelectionListener;
    private JPanel contentPanel;
    private JLabel testCandidateName;
    private JButton generateTestCaseButton;
    private JPanel labelPanel;
    private JPanel buttonPanel;
    private JLabel candidateNumber;

    private Dimension contentPanelDimensions = new Dimension(-1,30);

    public TestCandidateMetadataView(
            TestCandidateMetadata testCandidateMetadata,
            TestCaseService testCaseService,
            TestSelectionListener candidateSelectionListener,
            SessionInstance sessionInstance
    ) {
        this.testCandidateMetadata = testCandidateMetadata;
        this.testCaseService = testCaseService;
        this.candidateSelectionListener = candidateSelectionListener;
        this.contentPanel.setMaximumSize(contentPanelDimensions);
        this.contentPanel.setMaximumSize(contentPanelDimensions);

        this.generateTestCaseButton.setContentAreaFilled(false);
        this.generateTestCaseButton.setOpaque(false);
        this.generateTestCaseButton.setBorderPainted(false);


        MethodCallExpression mainMethod = (MethodCallExpression) testCandidateMetadata.getMainMethod();

        long entryTime=mainMethod.getEntryProbe().getNanoTime();
        long callTime=testCandidateMetadata.getCallTimeNanoSecond();
        long timeInMs=TimeUnit.NANOSECONDS.toMillis(callTime);
//        long timeS=TimeUnit.NANOSECONDS.toSeconds(71314710088602L);
//        Date date = new Date("71314710088602");
//        Format format = new SimpleDateFormat("yyyy MM dd HH:mm:ss");
//        String entryDateTimeStamp =  format.format(date);
        this.candidateNumber.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        testCandidateName.setText(mainMethod.getMethodName() + " at " + mainMethod.getEntryProbe().getNanoTime() + " | "+getMethodCallsForCandidate(this.testCandidateMetadata,sessionInstance,mainMethod)+ " Methods called, "+timeInMs+" ms | ");
        generateTestCaseButton.addActionListener(e -> generateTestCase());
    }

    private void generateTestCase() {
        candidateSelectionListener.onSelect(testCandidateMetadata);
    }

    public Component getContentPanel() {
        return contentPanel;
    }

    public Dimension getContentPanelDimensions() {
        return contentPanelDimensions;
    }

    public void setContentPanelDimensions(Dimension contentPanelDimensions) {
        this.contentPanelDimensions = contentPanelDimensions;
    }

    public void setCandidateNumberIndex(int candidateNumberIndex) {
        this.candidateNumber.setText("Candidate "+candidateNumberIndex);
    }

    private int getMethodCallsForCandidate(TestCandidateMetadata testCandidateMetadata, SessionInstance sessionInstance, MethodCallExpression mainMethod)
    {
        List<TestCandidateMetadata> candidates = sessionInstance.getTestCandidatesUntil(testCandidateMetadata.getTestSubject().getValue(),
                testCandidateMetadata.getCallTimeNanoSecond(), mainMethod.getId(), false);

        TestCandidateMetadata candidate = sessionInstance.getTestCandidateById(candidates.get(candidates.size()-1).getEntryProbeIndex());
        int methodCalls = candidate.getCallsList().size();

        return methodCalls;
    }
}
