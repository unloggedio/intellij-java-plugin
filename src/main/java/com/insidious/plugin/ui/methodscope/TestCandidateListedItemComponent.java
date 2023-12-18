package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.callbacks.CandidateLifeListener;
import com.insidious.plugin.callbacks.ExecutionRequestSourceType;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.pojo.ReplayAllExecutionContext;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.util.AtomicAssertionUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.ParameterUtils;
import com.insidious.plugin.util.UIUtils;
import com.intellij.openapi.diagnostic.Logger;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestCandidateListedItemComponent {
    public static final String TEST_FAIL_LABEL = "Fail";
    public static final String TEST_PASS_LABEL = "Pass";
    public static final String TEST_EXCEPTION_LABEL = "Exception";
    private static final Logger logger = LoggerUtil.getInstance(TestCandidateListedItemComponent.class);
    private final List<String> methodArgumentValues;
    private final Map<String, ArgumentNameValuePair> parameterMap;
    private final CandidateLifeListener candidateLifeListener;
    private StoredCandidate candidateMetadata;
    private JPanel mainPanel;
    private JLabel statusLabel;
    private JPanel mainContentPanel;
    private JButton executeLabel;
    private JPanel controlPanel;
    private JButton generateJunitLabel;
    private JButton saveReplayButton;
    private JPanel parameterPanel;
    private JPanel controlContainer;
    private JLabel assertionCountLabel;

    public TestCandidateListedItemComponent(
            StoredCandidate storedCandidate,
            List<ArgumentNameValuePair> argumentNameValuePairs,
            CandidateLifeListener candidateLifeListener,
            InsidiousService insidiousService,
            MethodAdapter method) {
        this.candidateMetadata = storedCandidate;
        this.candidateLifeListener = candidateLifeListener;
        this.methodArgumentValues = candidateMetadata.getMethodArguments();
        this.parameterMap = generateParameterMap(argumentNameValuePairs);
        parameterPanel.setLayout(new BorderLayout());

        //saved candidate check
        if (candidateMetadata.getName() != null && candidateMetadata.getName().length() > 0) {
            setTitledBorder(candidateMetadata.getName());
        } else {
            if (candidateMetadata.getCandidateId() != null) {
                setTitledBorder("#Saved candidate with no name");
            }
        }


        mainPanel.revalidate();

        executeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                insidiousService.chooseClassImplementation(method.getContainingClass().getQualifiedName(), psiClass -> {
                    JSONObject eventProperties = new JSONObject();
                    eventProperties.put("className", psiClass.getQualifiedClassName());
                    eventProperties.put("methodName", storedCandidate.getMethod().getName());
                    UsageInsightTracker.getInstance().RecordEvent("REXECUTE_SINGLE", eventProperties);
                    statusLabel.setText("Executing");
                    candidateLifeListener.executeCandidate(
                            Collections.singletonList(candidateMetadata), psiClass,
                            new ReplayAllExecutionContext(ExecutionRequestSourceType.Single, false),
                            (candidateMetadata, agentCommandResponse, diffResult) -> {
                                insidiousService.updateMethodHashForExecutedMethod(method);
                                candidateLifeListener.onCandidateSelected(new StoredCandidate(candidateMetadata));
                                insidiousService.triggerGutterIconReload();
                            }
                    );
                });
            }
        });
        generateJunitLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                candidateLifeListener.onGenerateJunitTestCaseRequest(candidateMetadata);
            }
        });
        saveReplayButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                AgentCommandResponse<String> agentCommandResponse = new AgentCommandResponse<>();
                agentCommandResponse.setResponseClassName(candidateMetadata.getReturnValueClassname());
                agentCommandResponse.setMethodReturnValue(candidateMetadata.getReturnValue());
                agentCommandResponse.setResponseType(ResponseType.NORMAL);

                candidateLifeListener.onSaveRequest(candidateMetadata, agentCommandResponse);
            }
        });

        statusLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!statusLabel.getText().trim().isEmpty()) {
                    candidateLifeListener.onCandidateSelected(candidateMetadata);
                }
            }
        });
        statusLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        executeLabel.setIcon(UIUtils.EXECUTE_ICON_OUTLINED_SVG);
        saveReplayButton.setIcon(UIUtils.SAVE_CANDIDATE_GREEN_SVG);
        generateJunitLabel.setIcon(UIUtils.TEST_CASES_ICON_PINK);

        executeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveReplayButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        generateJunitLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        mainPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                candidateLifeListener.onCandidateSelected(candidateMetadata);
            }
        });

        mainPanel.setBackground(UIUtils.agentResponseBaseColor);
        parameterPanel.setOpaque(false);
        controlPanel.setOpaque(false);


        MethodUnderTest methodUnderTest = candidateMetadata.getMethod();

        String itemLabel = String.format("<html>%s.<bold>%s</bold>() <small>d ms</small></html>",
                methodUnderTest.getClassName(), methodUnderTest.getName());
        Component methodNameLabel = new JLabel(
                itemLabel);
        parameterPanel.add(methodNameLabel);


    }

    public JPanel getComponent() {
        return this.mainPanel;
    }

    public StoredCandidate getCandidateMetadata() {
        return candidateMetadata;
    }

    public Map<String, ArgumentNameValuePair> generateParameterMap(List<ArgumentNameValuePair> argumentNameTypeList) {
        Map<String, ArgumentNameValuePair> argumentList = new HashMap<>();

        for (int i = 0; i < argumentNameTypeList.size(); i++) {
            ArgumentNameValuePair methodParameter = argumentNameTypeList.get(i);
            String parameterValue = methodArgumentValues == null || methodArgumentValues.size() <= i
                    ? "" : methodArgumentValues.get(i);
            String argumentTypeCanonicalName = methodParameter.getType();
            try {

                if (argumentTypeCanonicalName.equals("float") ||
                        argumentTypeCanonicalName.equals("java.lang.Float")) {
                    parameterValue = ParameterUtils.getFloatValue(parameterValue);
                } else if (argumentTypeCanonicalName.equals("double") ||
                        argumentTypeCanonicalName.equals("java.lang.Double")) {
                    parameterValue = ParameterUtils.getDoubleValue(parameterValue);
                }
            } catch (Exception e) {
                logger.warn("Failed to parse double/float [" + parameterValue + "]", e);
            }

            argumentList.put(methodParameter.getName(),
                    new ArgumentNameValuePair(methodParameter.getName(), argumentTypeCanonicalName, parameterValue));
        }
        return argumentList;
    }

    public void setAndDisplayResponse(DifferenceResult differenceResult) {

        switch (differenceResult.getDiffResultType()) {
            case DIFF:
                this.statusLabel.setText(TEST_FAIL_LABEL);
                this.statusLabel.setForeground(UIUtils.red);
                this.statusLabel.setIcon(UIUtils.DIFF_GUTTER);
                break;
            case NO_ORIGINAL:
                break;
            case SAME:
                this.statusLabel.setText(TEST_PASS_LABEL);
                this.statusLabel.setForeground(UIUtils.green);
                this.statusLabel.setIcon(UIUtils.NO_DIFF_GUTTER);
                break;
            default:
                this.statusLabel.setText(TEST_EXCEPTION_LABEL);
                this.statusLabel.setIcon(UIUtils.ORANGE_EXCEPTION);
                this.statusLabel.setForeground(UIUtils.orange);
        }
    }

    public int getHash() {
        int hash = -1;
        if (this.candidateMetadata != null && this.methodArgumentValues != null) {
            String output = this.candidateMetadata.getReturnValue();
            String concat = this.methodArgumentValues + output;
            hash = concat.hashCode();
        }
        return hash;
    }

    public void setTitledBorder(String title) {
        TitledBorder titledBorder = (TitledBorder) mainPanel.getBorder();
        titledBorder.setTitle(title);
    }

    public String getExecutionStatus() {
        return this.statusLabel.getText();
    }

    public void setCandidate(StoredCandidate storedCandidate) {
        this.candidateMetadata = storedCandidate;
        setTitledBorder(storedCandidate.getName());
        if (assertionCountLabel != null) {
            int assertionCount = AtomicAssertionUtils.countAssertions(candidateMetadata.getTestAssertions());
            assertionCountLabel.setText(assertionCount + " assertions");
        }
    }

    public void setStatus(String statusText) {
        statusLabel.setText(statusText);
    }
}
