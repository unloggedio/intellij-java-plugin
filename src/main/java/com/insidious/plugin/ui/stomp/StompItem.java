package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.callbacks.ExecutionRequestSourceType;
import com.insidious.plugin.callbacks.TestCandidateLifeListener;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.atomic.ClassUnderTest;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.ui.InsidiousUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;

public class StompItem {
    public static final JBColor TAG_LABEL_BACKGROUND_GREY = new JBColor(new Color(235, 235, 238),
            new Color(235, 235, 238));
    public static final JBColor TAG_LABEL_TEXT_GREY = new JBColor(new Color(113, 128, 150, 255),
            new Color(113, 128, 150, 255));
    public static final int MAX_METHOD_NAME_LABEL_LENGTH = 20;
    public static final JBColor HOVER_HIGHLIGHT_COLOR = new JBColor(
            new Color(157, 187, 184),
            new Color(157, 187, 184));
    private static final Logger logger = LoggerUtil.getInstance(StompItem.class);
    private final TestCandidateLifeListener testCandidateLifeListener;
    private TestCandidateMetadata candidateMetadata;
    private JPanel mainPanel;
    private JLabel statusLabel;
    private JLabel pinLabel;
    private JLabel timeTakenMsLabel;
    private JLabel lineCoverageLabel;
    private JLabel candidateTitleLabel;
    private JLabel parameterCountLabel;
    private JLabel callsCountLabel;
    private JPanel detailPanel;
    private JPanel infoPanel;
    private JPanel titleLabelContainer;
    private JPanel metadataPanel;
    private JCheckBox selectCandidateCheckbox;
    private JLabel replaySingle;
    private JPanel controlPanel;
    private JPanel controlContainer;
    private boolean isPinned = false;

    public StompItem(
            TestCandidateMetadata testCandidateMetadata,
            TestCandidateLifeListener testCandidateLifeListener,
            InsidiousService insidiousService) {
        this.candidateMetadata = testCandidateMetadata;
        this.testCandidateLifeListener = testCandidateLifeListener;
        Color defaultPanelColor = mainPanel.getBackground();

        MouseAdapter methodNameClickListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (!candidateMetadata.getLineNumbers().isEmpty()) {
                    Integer firstLine = candidateMetadata.getLineNumbers().get(0);
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        InsidiousUtils.focusProbeLocationInEditor(firstLine,
                                candidateMetadata.getFullyQualifiedClassname(), insidiousService.getProject());
                    });
                }
            }
        };
        titleLabelContainer.addMouseListener(methodNameClickListener);

        candidateTitleLabel.addMouseListener(methodNameClickListener);
        mainPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (!candidateMetadata.getLineNumbers().isEmpty()) {
                    Integer firstLine = candidateMetadata.getLineNumbers().get(0);
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        InsidiousUtils.focusProbeLocationInEditor(firstLine,
                                candidateMetadata.getFullyQualifiedClassname(), insidiousService.getProject());
                    });
                }

            }

            @Override
            public void mouseEntered(MouseEvent e) {
                mainPanel.setBackground(HOVER_HIGHLIGHT_COLOR);
                detailPanel.setBackground(HOVER_HIGHLIGHT_COLOR);
                infoPanel.setBackground(HOVER_HIGHLIGHT_COLOR);
                titleLabelContainer.setBackground(HOVER_HIGHLIGHT_COLOR);
                controlPanel.setBackground(HOVER_HIGHLIGHT_COLOR);
                controlContainer.setBackground(HOVER_HIGHLIGHT_COLOR);
                selectCandidateCheckbox.setBackground(HOVER_HIGHLIGHT_COLOR);
                metadataPanel.setBackground(HOVER_HIGHLIGHT_COLOR);
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mainPanel.setBackground(defaultPanelColor);
                detailPanel.setBackground(defaultPanelColor);
                infoPanel.setBackground(defaultPanelColor);
                titleLabelContainer.setBackground(defaultPanelColor);
                controlPanel.setBackground(defaultPanelColor);
                controlContainer.setBackground(defaultPanelColor);
                selectCandidateCheckbox.setBackground(defaultPanelColor);
                metadataPanel.setBackground(defaultPanelColor);
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
        pinLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isPinned) {
                    isPinned = false;
                    pinLabel.setIcon(UIUtils.PUSHPIN_LINE);
                } else {
                    isPinned = true;
                    pinLabel.setIcon(UIUtils.PUSHPIN_2_FILL);
                }
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
        pinLabel.setIcon(UIUtils.PUSHPIN_LINE);
        pinLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        replaySingle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        replaySingle.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    testCandidateLifeListener.executeCandidate(Collections.singletonList(candidateMetadata),
                            new ClassUnderTest(candidateMetadata.getFullyQualifiedClassname()),
                            ExecutionRequestSourceType.Single,
                            (testCandidate, agentCommandResponse, diffResult) -> {

                            });
                });
            }
        });
        pinLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isPinned) {
                    pinLabel.setIcon(UIUtils.UNPIN_LINE);
                } else {
                    pinLabel.setIcon(UIUtils.PUSHPIN_2_LINE);
                }
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (isPinned) {
                    pinLabel.setIcon(UIUtils.PUSHPIN_2_FILL);
                } else {
                    pinLabel.setIcon(UIUtils.PUSHPIN_LINE);
                }
                super.mouseExited(e);
            }
        });

        mainPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
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
        String itemLabel = String.format("%s()", methodUnderTest.getName());
        if (itemLabel.length() > MAX_METHOD_NAME_LABEL_LENGTH) {
            itemLabel = itemLabel.substring(0, MAX_METHOD_NAME_LABEL_LENGTH - 3) + "...";
        }


        ExecutionTimeCategory category = ExecutionTimeCategorizer.categorizeExecutionTime(timeTakenMs);
        String timeTakenMsString = ExecutionTimeCategorizer.formatTimePeriod(timeTakenMs);


        TitledBorder detailPanelBorder = (TitledBorder) mainPanel.getBorder();
        detailPanelBorder.setTitle(itemLabel);
//        candidateTitleLabel.setText(itemLabel);
        candidateTitleLabel.setToolTipText(
                className + "." + methodUnderTest.getName() + methodUnderTest.getSignature());

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

        callsCountLabel.setToolTipText("Click to show downstream calls");
        callsCountLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        callsCountLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                testCandidateLifeListener.onExpandChildren(candidateMetadata);
                super.mouseClicked(e);
            }
        });

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

    public void setSelected(boolean b) {
        selectCandidateCheckbox.setSelected(b);
    }

    public boolean isPinned() {
        return isPinned;
    }
}
