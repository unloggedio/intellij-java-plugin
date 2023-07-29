package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.ParameterAdapter;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.ui.IOTreeCellRenderer;
import com.insidious.plugin.ui.MethodExecutionListener;
import com.insidious.plugin.util.AtomicAssertionUtils;
import com.insidious.plugin.util.ClassUtils;
import com.insidious.plugin.util.JsonTreeUtils;
import com.insidious.plugin.util.UIUtils;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

public class TestCandidateListedItemComponent {
    private final MethodAdapter method;
    private final List<String> methodArgumentValues;
    private final MethodExecutionListener methodExecutionListener;
    private final Map<String, String> parameterMap;
    private final InsidiousService insidiousService;
    private StoredCandidate candidateMetadata;
    private JPanel mainPanel;
    private JLabel statusLabel;
    private JPanel mainContentPanel;
    private JLabel executeLabel;
    private JPanel controlPanel;

    public TestCandidateListedItemComponent(
            StoredCandidate storedCandidate,
            MethodAdapter method,
            MethodExecutionListener methodExecutionListener,
            CandidateSelectedListener candidateSelectedListener) {
        this.candidateMetadata = storedCandidate;
        this.insidiousService = method.getProject().getService(InsidiousService.class);
        this.method = method;
        this.methodExecutionListener = methodExecutionListener;
        this.methodArgumentValues = candidateMetadata.getMethodArguments();
        this.parameterMap = generateParameterMap(method.getParameters());
        mainContentPanel.setLayout(new BorderLayout());

        //saved candidate check
        if (candidateMetadata.getName() != null) {
            setTitledBorder(candidateMetadata.getName());
        }


        mainPanel.revalidate();

        loadInputTree();
        executeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ClassUtils.chooseClassImplementation(method.getContainingClass(), psiClass -> {
                    JSONObject eventProperties = new JSONObject();
                    eventProperties.put("className", psiClass.getQualifiedName());
                    eventProperties.put("methodName", method.getName());
                    UsageInsightTracker.getInstance().RecordEvent("REXECUTE_SINGLE", eventProperties);
                    statusLabel.setText("Executing");
                    methodExecutionListener.executeCandidate(
                            Collections.singletonList(candidateMetadata), psiClass, "individual",
                            (candidateMetadata, agentCommandResponse, diffResult) -> {
                                insidiousService.updateMethodHashForExecutedMethod(method);
//                                setAndDisplayResponse(agentCommandResponse, diffResult);
                                candidateSelectedListener.onCandidateSelected(candidateMetadata);
                                insidiousService.triggerGutterIconReload();
                            }
                    );
                });
            }
        });
        statusLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                candidateSelectedListener.onCandidateSelected(candidateMetadata);
            }
        });
        executeLabel.setIcon(UIUtils.EXECUTE_COMPONENT);
        statusLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        executeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        mainPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                candidateSelectedListener.onCandidateSelected(candidateMetadata);
            }
        });
    }

    public JPanel getComponent() {
        return this.mainPanel;
    }

    public StoredCandidate getCandidateMetadata() {
        return candidateMetadata;
    }

    private void loadInputTree() {

        mainContentPanel.removeAll();
        DefaultMutableTreeNode inputRoot = new DefaultMutableTreeNode("");
        Set<String> methodArgumentNames = this.parameterMap.keySet();
        int methodArgumentCount = methodArgumentNames.size();
        if (methodArgumentCount == 0) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode("No inputs for this method.");
            inputRoot.add(node);
        } else {
            for (String key : methodArgumentNames) {
                DefaultMutableTreeNode node = JsonTreeUtils.buildJsonTree(this.parameterMap.get(key), key);
                inputRoot.add(node);
            }
        }

        JTree inputTree = new Tree(inputRoot);
        inputTree.setBorder(JBUI.Borders.empty());
        inputTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                inputTree.clearSelection();
            }
        });


        inputTree.setCellRenderer(new IOTreeCellRenderer());
        inputTree.setRootVisible(false);
        inputTree.setShowsRootHandles(true);

//        GridLayout gridLayout = new GridLayout(1, 1);
        int desiredHeightPerInput = 30;
        int desiredHeight = inputRoot.getLeafCount() * desiredHeightPerInput;
        if (desiredHeight < 100) {
            desiredHeight = 100;
        }
        if (desiredHeight > 220) {
            desiredHeight = 220;
        }
        Dimension preferredSize = new Dimension(-1, desiredHeight);

        inputTree.setSize(new Dimension(-1, desiredHeight));
        inputTree.setMaximumSize(new Dimension(-1, desiredHeight));

        JScrollPane scrollPane = new JBScrollPane(inputTree);
        scrollPane.setBorder(JBUI.Borders.empty());

        scrollPane.setPreferredSize(new Dimension(-1, desiredHeight));
        scrollPane.setMaximumSize(new Dimension(-1, desiredHeight));
        scrollPane.setSize(new Dimension(-1, desiredHeight));

        mainPanel.setPreferredSize(preferredSize);
        mainPanel.setMinimumSize(new Dimension(-1, 100));
        mainPanel.setMaximumSize(preferredSize);

        mainContentPanel.setSize(new Dimension(-1, desiredHeight));

        mainContentPanel.setMaximumSize(preferredSize);
        mainContentPanel.setPreferredSize(preferredSize);
        mainContentPanel.add(scrollPane, BorderLayout.CENTER);

        JLabel argumentsLabel = new JLabel("Method arguments");
        JPanel argumentsLabelPanel = new JPanel();
        argumentsLabelPanel.setLayout(new BorderLayout());
        argumentsLabelPanel.add(argumentsLabel, BorderLayout.WEST);

        if (candidateMetadata.getCandidateId() != null && candidateMetadata.getTestAssertions() != null) {
            int assertionCount = AtomicAssertionUtils.countAssertions(candidateMetadata.getTestAssertions());
            JLabel assertionCountLabel = new JLabel(assertionCount + " assertions");
            JPanel countPanel = new JPanel(new BorderLayout());
            countPanel.add(assertionCountLabel, BorderLayout.WEST);
            mainContentPanel.add(countPanel, BorderLayout.SOUTH);
        }
        mainContentPanel.add(argumentsLabelPanel, BorderLayout.NORTH);


        mainContentPanel.revalidate();
        mainContentPanel.repaint();
    }

    public Map<String, String> getParameterMap() {
        if (this.parameterMap == null || this.parameterMap.size() == 0) {
            return generateParameterMap(method.getParameters());
        } else {
            return this.parameterMap;
        }
    }

    public Map<String, String> generateParameterMap(ParameterAdapter[] parameters) {
        Map<String, String> parameterInputMap = new TreeMap<>();
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                ParameterAdapter methodParameter = parameters[i];
                String parameterValue = methodArgumentValues == null || methodArgumentValues.size() <= i ? "" :
                        methodArgumentValues.get(i);
                parameterInputMap.put(methodParameter.getName(), parameterValue);
            }
        }
        return parameterInputMap;
    }

    public void setAndDisplayResponse(
            AgentCommandResponse<String> agentCommandResponse,
            DifferenceResult differenceResult
    ) {

        switch (differenceResult.getDiffResultType()) {
            case DIFF:
                this.statusLabel.setText("Different");
                this.statusLabel.setForeground(UIUtils.red);
                this.statusLabel.setIcon(UIUtils.DIFF_GUTTER);
                break;
            case NO_ORIGINAL:
                break;
            case SAME:
                this.statusLabel.setText("Same");
                this.statusLabel.setForeground(UIUtils.green);
                this.statusLabel.setIcon(UIUtils.NO_DIFF_GUTTER);
                break;
            default:
                this.statusLabel.setText("Exception");
                this.statusLabel.setIcon(UIUtils.ORANGE_EXCEPTION);
                this.statusLabel.setForeground(UIUtils.orange);
                System.out.println("Exception display for status label : ");
        }
    }

    public int getHash() {
        int hash = -1;
        if (this.candidateMetadata != null && this.methodArgumentValues != null) {
            String output = this.candidateMetadata.getReturnValue();
            String concat = this.methodArgumentValues.toString() + output;
            hash = concat.toString().hashCode();
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
    }
}
