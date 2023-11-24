package com.insidious.plugin.ui.methodscope;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.callbacks.CandidateLifeListener;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.util.*;
import com.intellij.openapi.diagnostic.Logger;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.insidious.plugin.util.ParameterUtils.processResponseForFloatAndDoubleTypes;

public class AgentResponseComponent implements ResponsePreviewComponent {
    public static final ObjectMapper objectMapper = ObjectMapperInstance.getInstance();
    private static final Logger logger = LoggerUtil.getInstance(AgentResponseComponent.class);
    private static final boolean SHOW_TEST_CASE_CREATE_BUTTON = false;
    private final AgentCommandResponse<String> agentCommandResponse;
//    private JButton createTestCaseButton;
    private StoredCandidate testCandidate;
    private JPanel mainPanel;
    private JPanel centerPanel;
    private JButton viewFullButton;
    private JTable mainTable;
    private JLabel statusLabel;
    private JPanel tableParent;
    private JButton acceptButton;
    private JPanel topAlign;
    private JButton deleteButton;
    private JPanel buttonRightPanel;
    private JScrollPane scrollParent;
    private JPanel statusPanel;
    private JPanel informationPanel;
    private JLabel informationLabel;
    private JPanel mainContentPanel;
    private JPanel newBottomPanel;
    private JPanel topRightbuttonPanel;

    public AgentResponseComponent(
            AgentCommandResponse<String> agentCommandResponse,
            StoredCandidate testCandidate,
            FullViewEventListener fullViewEventListener,
            CandidateLifeListener candidateLifeListener
    ) {
        this.agentCommandResponse = agentCommandResponse;
        this.testCandidate = testCandidate;
        topRightbuttonPanel.setVisible(false);

        DifferenceResult differences = DiffUtils.calculateDifferences(this.testCandidate, agentCommandResponse);
        computeDifferences(differences);

        String originalString;
        if (this.testCandidate.isReturnValueIsBoolean() && DiffUtils.isNumeric(this.testCandidate.getReturnValue())) {
            originalString = "0".equals(this.testCandidate.getReturnValue()) ? "false" : "true";
        } else {
            originalString = this.testCandidate.getReturnValue();
        }
        String actualString = String.valueOf(agentCommandResponse.getMethodReturnValue());

        String simpleClassName = agentCommandResponse.getTargetClassName();
        simpleClassName = simpleClassName.substring(simpleClassName.lastIndexOf(".") + 1);

        String methodLabel = simpleClassName + "." + agentCommandResponse.getTargetMethodName() + "()";

        setInfoLabel("Replayed at " + DateUtils.formatDate(new Date(agentCommandResponse.getTimestamp())) +
                " for " + methodLabel);
        String processedOriginal = processResponseForFloatAndDoubleTypes(agentCommandResponse.getResponseClassName(), originalString);
        String processedActual = processResponseForFloatAndDoubleTypes(agentCommandResponse.getResponseClassName(), actualString);
        viewFullButton.addActionListener(
                e -> fullViewEventListener.generateCompareWindows(processedOriginal, processedActual));

//        if (SHOW_TEST_CASE_CREATE_BUTTON) {
//            createTestCaseButton = new JButton("Create JUnit test case");
//            createTestCaseButton.setOpaque(false);
//            createTestCaseButton.setContentAreaFilled(false);
//            createTestCaseButton.setIcon(UIUtils.TEST_CASES_ICON_PINK);
//            buttonRightPanel.add(createTestCaseButton);
//            if (testCandidate.getEntryProbeIndex() < 1) {
//                createTestCaseButton.setEnabled(false);
//                createTestCaseButton.setText("Test case generation waiting for scan");
//            }
//            createTestCaseButton.addActionListener(
//                    e -> candidateLifeListener.onGenerateJunitTestCaseRequest(testCandidate));
//        createTestCaseButton.setBorder(new LineBorder(UIUtils.buttonBorderColor));
//        }

        acceptButton.addActionListener(
                e -> candidateLifeListener.onSaveRequest(this.testCandidate, agentCommandResponse));
        if (this.testCandidate.getCandidateId() == null) {
            deleteButton.setVisible(false);
        }
        deleteButton.addActionListener(e -> candidateLifeListener.onDeleteRequest(this.testCandidate));

        deleteButton.setIcon(UIUtils.DELETE_CANDIDATE_RED_SVG);
        acceptButton.setIcon(UIUtils.SAVE_CANDIDATE_GREEN_SVG);

        deleteButton.setBorder(new LineBorder(UIUtils.buttonBorderColor));
        acceptButton.setBorder(new LineBorder(UIUtils.buttonBorderColor));
        viewFullButton.setBorder(new LineBorder(UIUtils.buttonBorderColor));
        mainContentPanel.setBackground(UIUtils.agentResponseBaseColor);
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
                this.statusLabel.setText("Failing");
                this.statusLabel.setIcon(UIUtils.DIFF_GUTTER);
                renderTableWithDifferences(differenceResult.getDifferenceInstanceList());
                break;
            case NO_ORIGINAL:
                this.statusLabel.setText("No previous Candidate found, current response.");
                renderTableForResponse(differenceResult.getRightOnly());
                break;
            case SAME:
                this.statusLabel.setText("Passing");
                this.statusLabel.setIcon(UIUtils.CHECK_GREEN_SMALL);
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
        this.mainTable.setBackground(UIUtils.agentResponseBaseColor);
        this.scrollParent.getViewport().setOpaque(true);
        this.scrollParent.getViewport().setBackground(UIUtils.agentResponseBaseColor);
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

    @Override
    public void setTestCandidate(StoredCandidate candidate) {
        this.testCandidate = candidate;
//        if (testCandidate.getEntryProbeIndex() > 1 && !createTestCaseButton.isEnabled()) {
//            createTestCaseButton.setEnabled(true);
//            createTestCaseButton.setText("Create JUnit test case");
//        }
    }

    @Override
    public StoredCandidate getTestCandidate() {
        return testCandidate;
    }

}
