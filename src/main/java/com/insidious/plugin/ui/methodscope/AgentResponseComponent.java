package com.insidious.plugin.ui.methodscope;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.callbacks.CandidateLifeListener;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.ui.Components.ResponseMapTable;
import com.insidious.plugin.util.*;
import com.intellij.openapi.diagnostic.Logger;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class AgentResponseComponent implements Supplier<Component> {
    public static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerUtil.getInstance(AgentResponseComponent.class);
    private static final boolean SHOW_TEST_CASE_CREATE_BUTTON = true;
    private final AgentCommandResponse<String> agentCommandResponse;
    private final StoredCandidate testCandidate;
    private JPanel mainPanel;
    private JPanel centerPanel;
    private JButton viewFullButton;
    private JTable mainTable;
    private JLabel statusLabel;
    private JPanel tableParent;
    private JButton acceptButton;
    private JPanel topAlign;
    private JButton deleteButton;
    private JPanel buttonPanel;
    private JScrollPane scrollParent;
    private JPanel statusPanel;
    private JPanel informationPanel;
    private JLabel informationLabel;
    private JPanel mainContentPanel;

    public AgentResponseComponent(
            AgentCommandResponse<String> agentCommandResponse,
            StoredCandidate storedCandidate,
            FullViewEventListener fullViewEventListener,
            CandidateLifeListener candidateLifeListener
    ) {
        this.agentCommandResponse = agentCommandResponse;
        this.testCandidate = storedCandidate;

        DifferenceResult differences = DiffUtils.calculateDifferences(testCandidate, agentCommandResponse);
        computeDifferences(differences);

        String originalString;
        if (testCandidate.isReturnValueIsBoolean() && DiffUtils.isNumeric(testCandidate.getReturnValue())) {
            originalString = "0".equals(testCandidate.getReturnValue()) ? "false" : "true";
        } else {
            originalString = testCandidate.getReturnValue();
        }
        String actualString = String.valueOf(agentCommandResponse.getMethodReturnValue());

        String simpleClassName = agentCommandResponse.getTargetClassName();
        simpleClassName = simpleClassName.substring(simpleClassName.lastIndexOf(".") + 1);

        String methodLabel = simpleClassName + "." + agentCommandResponse.getTargetMethodName() + "()";

        setInfoLabel("Replayed at " + DateUtils.formatDate(new Date(agentCommandResponse.getTimestamp())) +
                " for " + methodLabel);
        viewFullButton.addActionListener(
                e -> fullViewEventListener.generateCompareWindows(originalString, actualString));

//        if (SHOW_TEST_CASE_CREATE_BUTTON) {
//            JButton createTestCaseButton = new JButton("Create test case");
//            bottomControlPanel.add(createTestCaseButton, new GridConstraints());
//            createTestCaseButton.addActionListener(new ActionListener() {
//                @Override
//                public void actionPerformed(ActionEvent e) {
//                    logger.warn("Create test case: " + testCandidateMetadata);
//
//                    TestCandidateMetadata loadedTestCandidate = insidiousService.getSessionInstance()
//                            .getTestCandidateById(testCandidateMetadata.getEntryProbeIndex(), true);
//
//
//                    String testMethodName =
//                            "testMethod" + ClassTypeUtils.upperInstanceName(
//                                    loadedTestCandidate.getMainMethod().getMethodName());
//                    TestCaseGenerationConfiguration testCaseGenerationConfiguration = new TestCaseGenerationConfiguration(
//                            TestFramework.JUnit5,
//                            MockFramework.Mockito,
//                            JsonFramework.JACKSON,
//                            ResourceEmbedMode.IN_FILE
//                    );
//
//
//                    // mock all calls by default
//                    testCaseGenerationConfiguration.getCallExpressionList()
//                            .addAll(loadedTestCandidate.getCallsList());
//
//
//                    testCaseGenerationConfiguration.setTestMethodName(testMethodName);
//
//
//                    testCaseGenerationConfiguration.getTestCandidateMetadataList().clear();
//                    testCaseGenerationConfiguration.getTestCandidateMetadataList().add(loadedTestCandidate);
//
//
//                    try {
//                        insidiousService.generateAndSaveTestCase(testCaseGenerationConfiguration);
//                    } catch (Exception ex) {
//                        InsidiousNotification.notifyMessage("Failed to generate test case: " + ex.getMessage(),
//                                NotificationType.ERROR);
//                    }
//                }
//            });
//        }

        acceptButton.addActionListener(e -> candidateLifeListener.onSaveRequest(testCandidate, agentCommandResponse));
        if (testCandidate.getCandidateId() == null) {
            deleteButton.setVisible(false);
        }
        deleteButton.addActionListener(e -> candidateLifeListener.onDeleteRequest(testCandidate));

        deleteButton.setIcon(UIUtils.DELETE_CANDIDATE_RED_SVG);
        acceptButton.setIcon(UIUtils.SAVE_CANDIDATE_GREEN_SVG);
    }

    public void setInfoLabel(String info) {
        informationLabel.setText(info);
        informationLabel.setIcon(UIUtils.REPLAY_PINK);
        TitledBorder border = BorderFactory.createTitledBorder("");
        mainContentPanel.setBorder(border);
    }

    public void computeDifferences(DifferenceResult differenceResult) {

        switch (differenceResult.getDiffResultType()) {
            case DIFF:
                this.statusLabel.setText("Failing.");
                this.statusLabel.setIcon(UIUtils.DIFF_GUTTER);
                this.statusLabel.setForeground(UIUtils.red);
                renderTableWithDifferences(differenceResult.getDifferenceInstanceList());
                break;
            case NO_ORIGINAL:
                this.statusLabel.setText("No previous Candidate found, current response.");
                renderTableForResponse(differenceResult.getRightOnly());
                break;
            case SAME:
                this.statusLabel.setText("Passing.");
                this.statusLabel.setIcon(UIUtils.CHECK_GREEN_SMALL);
                this.statusLabel.setForeground(UIUtils.green);
                this.tableParent.setVisible(false);
                break;
            default:
                this.statusLabel.setText("" + this.agentCommandResponse.getMessage());
                this.statusLabel.setIcon(UIUtils.EXCEPTION_CASE);
                this.statusLabel.setForeground(UIUtils.red);
                this.tableParent.setVisible(false);
                showExceptionTrace(
                        ExceptionUtils.prettyPrintException(
                                this.testCandidate.getReturnValue())
                );
                break;
        }
    }

    private void renderTableWithDifferences(List<DifferenceInstance> differenceInstances) {
        CompareTableModel newModel = new CompareTableModel(differenceInstances, this.mainTable);
        this.mainTable.setModel(newModel);
        this.mainTable.revalidate();
        this.mainTable.repaint();
    }

    private void renderTableForResponse(Map<String, Object> rightOnly) {
        ObjectNode objectNode;
        try {
            objectNode = JsonTreeUtils.flatten(objectMapper.readTree(objectMapper.writeValueAsString(rightOnly)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        ResponseMapTable newModel = new ResponseMapTable(objectNode);
        this.mainTable.setModel(newModel);
        this.mainTable.revalidate();
        this.mainTable.repaint();
    }

    public void showExceptionTrace(String response) {
        this.tableParent.removeAll();
        JTextArea textArea = new JTextArea();
        textArea.setText(response);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        this.tableParent.add(textArea, BorderLayout.CENTER);
        this.tableParent.revalidate();
    }

    @Override
    public Component get() {
        return this.mainPanel;
    }

}
