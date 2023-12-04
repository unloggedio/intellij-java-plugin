package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.callbacks.TestCandidateLifeListener;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class StompItem {
    public static final JBColor TAG_LABEL_BACKGROUND_GREY = new JBColor(new Color(232, 232, 236),
            new Color(232, 232, 236));
    public static final JBColor TAG_LABEL_TEXT_GREY = new JBColor(new Color(113, 128, 150, 255),
            new Color(113, 128, 150, 255));
    private static final Logger logger = LoggerUtil.getInstance(StompItem.class);
    private final TestCandidateLifeListener storedCandidateLifeListener;
    private TestCandidateMetadata candidateMetadata;
    private JPanel mainPanel;
    private JLabel statusLabel;
    private JLabel generateJunitLabel;
    private JLabel timeTakenMsLabel;
    private JLabel lineCoverageLabel;
    private JLabel candidateTitleLabel;
    private JLabel parameterCountLabel;
    private JLabel callsCountLabel;
    private JPanel detailPanel;
    private JPanel infoPanel;
    private JPanel titleLabelContainer;
    private JPanel controlPanel;
    private JPanel controlContainer;
    private JPanel metadataPanel;
    private JCheckBox selectCandidateCheckbox;

    public StompItem(
            TestCandidateMetadata testCandidateMetadata,
            TestCandidateLifeListener testCandidateLifeListener) {
        this.candidateMetadata = testCandidateMetadata;
        this.storedCandidateLifeListener = testCandidateLifeListener;
        Color defaultPanelColor = mainPanel.getBackground();

        mainPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
//                mainPanel.setBackground(JBColor.ORANGE);
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
//                mainPanel.setBackground(defaultPanelColor);
                super.mouseExited(e);
            }
        });

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

//        statusLabel.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                if (!statusLabel.getText().trim().isEmpty()) {
//                    testCandidateLifeListener.onCandidateSelected(candidateMetadata);
//                }
//            }
//        });
//        statusLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

//        executeLabel.setIcon(UIUtils.EXECUTE_ICON_OUTLINED_SVG);
//        saveReplayButton.setIcon(UIUtils.SAVE_CANDIDATE_GREEN_SVG);

//        executeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//        saveReplayButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        generateJunitLabel.setIcon(UIUtils.PUSHPIN_LINE);
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
//        setTitledBorder("[" + candidateMetadata.getEntryProbeIndex() + "] " + className);
        long timeTakenMs = (candidateMetadata.getMainMethod().getReturnValue().getProb().getRecordedAt() -
                candidateMetadata.getMainMethod().getEntryProbe().getRecordedAt()) / (1000 * 1000);
        String classNameColor = StringColorPicker.pickColor(methodUnderTest.getName());
        String itemLabel = String.format("<html><b>%s</b>()</html>", methodUnderTest.getName()
        );


        ExecutionTimeCategory category = ExecutionTimeCategorizer.categorizeExecutionTime(timeTakenMs);
        String timeTakenMsString = ExecutionTimeCategorizer.formatTimePeriod(timeTakenMs);

        candidateTitleLabel.setText(itemLabel);

        timeTakenMsLabel = createTagLabel("%s", timeTakenMsString, Color.decode(category.getColorHex()), JBColor.WHITE);
        metadataPanel.add(timeTakenMsLabel);

        if (candidateMetadata.getLineNumbers().size() > 1) {
            lineCoverageLabel = createTagLabel("+%d lines", candidateMetadata.getLineNumbers().size(),
                    TAG_LABEL_BACKGROUND_GREY, Color.decode(ExecutionTimeCategory.INSTANTANEOUS.getColorHex()));
            metadataPanel.add(lineCoverageLabel);
        }

        parameterCountLabel = createTagLabel("%d Argument" + (
                        candidateMetadata.getMainMethod().getArgumentProbes().size() > 1 ? "s" : ""
                ), candidateMetadata.getMainMethod().getArgumentProbes().size(), TAG_LABEL_BACKGROUND_GREY,
                TAG_LABEL_TEXT_GREY);
        metadataPanel.add(parameterCountLabel);

        callsCountLabel = createTagLabel("%d Downstream", candidateMetadata.getCallsList().size(),
                TAG_LABEL_BACKGROUND_GREY, TAG_LABEL_TEXT_GREY);
        metadataPanel.add(callsCountLabel);

        selectCandidateCheckbox.addActionListener(e -> {
            if (selectCandidateCheckbox.isSelected()) {
                testCandidateLifeListener.onSelected(candidateMetadata);
            } else {
                testCandidateLifeListener.unSelected(candidateMetadata);
            }
        });

    }

    private JLabel createTagLabel(String tagText, Object value, Color backgroundColor, Color foreground) {
        JLabel label = new JLabel();
        label.setText(String.format("<html><small>" + tagText + "</small></html>", value));

        label.setOpaque(true);
        JPopupMenu jPopupMenu = new JPopupMenu("Yay");
        jPopupMenu.add(new JMenuItem("item 1", UIUtils.GHOST_MOCK));
        label.setComponentPopupMenu(jPopupMenu);
        label.setBackground(backgroundColor);
        label.setForeground(foreground);

        // Creating a rounded border
        Border roundedBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(backgroundColor, 3, true),
                BorderFactory.createEmptyBorder(0, 2, 0, 2)
        );
        label.setBorder(roundedBorder);
        return label;
    }

    public JPanel getComponent() {
        return mainPanel;
    }

    public void setTitledBorder(String title) {
        TitledBorder titledBorder = (TitledBorder) mainPanel.getBorder();
        titledBorder.setTitle(title);
    }


    public void setStatus(String statusText) {
        statusLabel.setText(statusText);
    }

    public TestCandidateMetadata getTestCandidate() {
        return candidateMetadata;
    }
}
