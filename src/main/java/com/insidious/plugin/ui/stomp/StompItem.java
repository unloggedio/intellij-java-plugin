package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.callbacks.TestCandidateLifeListener;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
import com.intellij.openapi.diagnostic.Logger;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class StompItem {
    private static final Logger logger = LoggerUtil.getInstance(StompItem.class);
    private final TestCandidateLifeListener storedCandidateLifeListener;
    private TestCandidateMetadata candidateMetadata;
    private JPanel mainPanel;
    private JLabel statusLabel;
    private JLabel generateJunitLabel;
    private JLabel timeTakenMsLabel;
    private JLabel candidateTitleLabel;
    private JLabel parameterCountLabel;
    private JLabel callsCountLabel;
    private JPanel detailPanel;
    private JPanel infoPanel;
    private JPanel titleLabelContainer;
    private JPanel controlPanel;
    private JPanel controlContainer;
    private JPanel metadataPanel;

    public StompItem(
            TestCandidateMetadata testCandidateMetadata,
            TestCandidateLifeListener testCandidateLifeListener,
            InsidiousService insidiousService) {
        this.candidateMetadata = testCandidateMetadata;
        this.storedCandidateLifeListener = testCandidateLifeListener;


        mainPanel.revalidate();

//        executeLabel.addMouseListener(
//                new MouseAdapter() {
//                    @Override
//                    public void mouseClicked(MouseEvent e) {
//                        insidiousService.chooseClassImplementation(testCandidateMetadata.getFullyQualifiedClassname(),
//                                psiClass -> {
//                                    JSONObject eventProperties = new JSONObject();
//                                    eventProperties.put("className", psiClass.getQualifiedClassName());
//                                    UsageInsightTracker.getInstance().RecordEvent("REXECUTE_SINGLE", eventProperties);
//                                    statusLabel.setText("Executing");
//                                    testCandidateLifeListener.executeCandidate(
//                                            Collections.singletonList(candidateMetadata), psiClass, "individual",
//                                            (candidateMetadata, agentCommandResponse, diffResult) -> {
//                                                testCandidateLifeListener.onCandidateSelected(candidateMetadata);
//                                            }
//                                    );
//                                });
//                    }
//                });
        generateJunitLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                testCandidateLifeListener.onGenerateJunitTestCaseRequest(candidateMetadata);
            }
        });
//        saveReplayButton.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                AgentCommandResponse<String> agentCommandResponse = new AgentCommandResponse<>();
//                Parameter returnValue = candidateMetadata.getMainMethod().getReturnValue();
//                agentCommandResponse.setResponseClassName(returnValue.getType());
//                agentCommandResponse.setMethodReturnValue(returnValue.getStringValue() != null ?
//                        returnValue.getStringValue() : String.valueOf(returnValue.getValue()));
//                agentCommandResponse.setResponseType(ResponseType.NORMAL);
//
//                testCandidateLifeListener.onSaveRequest(candidateMetadata, agentCommandResponse);
//            }
//        });

        statusLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!statusLabel.getText().trim().isEmpty()) {
                    testCandidateLifeListener.onCandidateSelected(candidateMetadata);
                }
            }
        });
        statusLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

//        executeLabel.setIcon(UIUtils.EXECUTE_ICON_OUTLINED_SVG);
//        saveReplayButton.setIcon(UIUtils.SAVE_CANDIDATE_GREEN_SVG);

//        executeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//        saveReplayButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        generateJunitLabel.setIcon(UIUtils.COMPASS_DISCOVER_LINE);
        generateJunitLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        mainPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                testCandidateLifeListener.onCandidateSelected(candidateMetadata);
            }
        });

//        mainPanel.setBackground(UIUtils.agentResponseBaseColor);
//        detailPanel.setOpaque(false);
//        controlPanel.setOpaque(false);


        MethodUnderTest methodUnderTest = new MethodUnderTest(
                candidateMetadata.getMainMethod().getMethodName(), "",
                0, candidateMetadata.getFullyQualifiedClassname()
        );

        String className = methodUnderTest.getClassName();
        if (className.contains(".")) {
            className = className.substring(className.lastIndexOf(".") + 1);
        }
        setTitledBorder("[" + candidateMetadata.getEntryProbeIndex() + "] " + className);
        long timeTakenMs = (candidateMetadata.getMainMethod().getReturnValue().getProb().getRecordedAt() -
                candidateMetadata.getMainMethod().getEntryProbe().getRecordedAt()) / (1000 * 1000);
        String classNameColor = StringColorPicker.pickColor(methodUnderTest.getName());
        String itemLabel = String.format("<html><font color='%s'><b>%s</b></font>()</html>",
                classNameColor, methodUnderTest.getName()
        );


        ExecutionTimeCategory category = ExecutionTimeCategorizer.categorizeExecutionTime(timeTakenMs);
        String timeTakenMsString = ExecutionTimeCategorizer.formatTimePeriod(timeTakenMs);

        timeTakenMsLabel.setText(String.format(
                "<html>" + "<font color='%s'><u><small>%s</small></u>" + "</font></html>",
                category.getColorHex(), timeTakenMsString));

        candidateTitleLabel.setText(itemLabel);
        parameterCountLabel.setText(String.format("<html><u>%d Argument</u></html>",
                candidateMetadata.getMainMethod().getArgumentProbes().size()));

        callsCountLabel.setText(String.format("<html>%d Downstream</html>",
                candidateMetadata.getCallsList().size()));


    }

    public JPanel getComponent() {
        return this.mainPanel;
    }

    public void setTitledBorder(String title) {
        TitledBorder titledBorder = (TitledBorder) mainPanel.getBorder();
        titledBorder.setTitle(title);
    }

    public String getExecutionStatus() {
        return this.statusLabel.getText();
    }


    public void setStatus(String statusText) {
        statusLabel.setText(statusText);
    }

    public TestCandidateMetadata getTestCandidate() {
        return candidateMetadata;
    }
}
