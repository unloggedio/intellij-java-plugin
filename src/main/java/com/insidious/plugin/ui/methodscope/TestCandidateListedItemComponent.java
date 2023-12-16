package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.callbacks.CandidateLifeListener;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.pojo.ReplayAllExecutionContext;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.ui.IOTreeCellRenderer;
import com.insidious.plugin.util.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

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
    private JPanel controlContainer;
    private JButton generateJunitLabel;
    private JButton saveReplayButton;
    private JPanel parameterPanel;
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

        loadInputTree();
        executeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ClassUtils.chooseClassImplementation(method.getContainingClass(), psiClass -> {
                    JSONObject eventProperties = new JSONObject();
                    eventProperties.put("className", psiClass.getQualifiedClassName());
                    eventProperties.put("methodName", storedCandidate.getMethod().getName());
                    UsageInsightTracker.getInstance().RecordEvent("REXECUTE_SINGLE", eventProperties);
                    statusLabel.setText("Executing");
                    ReplayAllExecutionContext context = new ReplayAllExecutionContext("individual",
                            false);
                    candidateLifeListener.executeCandidate(
                            Collections.singletonList(candidateMetadata), psiClass, context,
                            (candidateMetadata, agentCommandResponse, diffResult) -> {
                                insidiousService.updateMethodHashForExecutedMethod(method);
                                candidateLifeListener.onCandidateSelected(candidateMetadata);
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
                //cause for 2 issues here
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


    }

    public JPanel getComponent() {
        return this.mainPanel;
    }

    public StoredCandidate getCandidateMetadata() {
        return candidateMetadata;
    }

    private void loadInputTree() {

        parameterPanel.removeAll();
        DefaultMutableTreeNode inputRoot = new DefaultMutableTreeNode("");
        Set<String> methodArgumentNames = this.parameterMap.values()
                .stream().map(ArgumentNameValuePair::getName)
                .collect(Collectors.toSet());
        int methodArgumentCount = methodArgumentNames.size();
        if (methodArgumentCount == 0) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode("No arguments");
            inputRoot.add(node);
        } else {
            for (String key : methodArgumentNames) {
                DefaultMutableTreeNode node = JsonTreeUtils.buildJsonTree(this.parameterMap.get(key).getValue(), key);
                inputRoot.add(node);
            }
        }

        JTree inputTree = new Tree(inputRoot);
        inputTree.setBorder(JBUI.Borders.empty());
        inputTree.addTreeSelectionListener(e -> inputTree.clearSelection());


        inputTree.setCellRenderer(new IOTreeCellRenderer());
        inputTree.setRootVisible(false);
        inputTree.setShowsRootHandles(true);

//        GridLayout gridLayout = new GridLayout(1, 1);
        int desiredHeightPerInput = 50;
        int desiredHeight = inputRoot.getLeafCount() * desiredHeightPerInput;
        if (desiredHeight < 150) {
            desiredHeight = 150;
        }
        if (desiredHeight > 250) {
            desiredHeight = 250;
        }

        inputTree.setSize(new Dimension(-1, desiredHeight));
        inputTree.setMaximumSize(new Dimension(-1, desiredHeight));

        JScrollPane scrollPane = new JBScrollPane(inputTree);

        scrollPane.setPreferredSize(new Dimension(-1, desiredHeight));
        scrollPane.setMaximumSize(new Dimension(-1, desiredHeight));
        scrollPane.setSize(new Dimension(-1, desiredHeight));

        mainPanel.setPreferredSize(new Dimension(-1, desiredHeight));
        mainPanel.setMinimumSize(new Dimension(-1, 100));
        mainPanel.setMaximumSize(new Dimension(-1, desiredHeight));

        parameterPanel.setSize(new Dimension(-1, desiredHeight));

        parameterPanel.setMaximumSize(new Dimension(-1, desiredHeight));
        parameterPanel.setPreferredSize(new Dimension(-1, desiredHeight));
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        inputTree.setOpaque(false);
//        inputTree.path
        inputTree.setBackground(UIUtils.agentResponseBaseColor);
        parameterPanel.add(scrollPane, BorderLayout.CENTER);

        if (candidateMetadata.getCandidateId() != null && candidateMetadata.getTestAssertions() != null) {
            int assertionCount = AtomicAssertionUtils.countAssertions(candidateMetadata.getTestAssertions());
            assertionCountLabel = new JLabel(assertionCount + " assertions");
            assertionCountLabel.setAlignmentY(1.0F);
            JPanel countPanel = new JPanel(new BorderLayout());

            Border border1 = countPanel.getBorder();
            Border margin1 = JBUI.Borders.emptyTop(5);
            CompoundBorder borderWithMargin1 = new CompoundBorder(border1, margin1);
            countPanel.setBorder(borderWithMargin1);

            countPanel.add(assertionCountLabel, BorderLayout.WEST);
            countPanel.setAlignmentY(1.0F);
            countPanel.setOpaque(false);
            parameterPanel.add(countPanel, BorderLayout.SOUTH);
        }
//        parameterPanel.add(argumentsLabelPanel, BorderLayout.NORTH);

        parameterPanel.revalidate();
        parameterPanel.repaint();
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

    public void refreshJunitButtonStatus() {
        //only needed if not available
        if (!this.generateJunitLabel.isEnabled()) {
            generateJunitLabel.setEnabled(getCanGenerateUnitCase());
        }
    }

    public void setJunitButtonEnableState(boolean state) {
        generateJunitLabel.setEnabled(state);
    }

    private boolean getCanGenerateUnitCase() {
        return candidateLifeListener.canGenerateUnitCase(candidateMetadata);
    }
}
