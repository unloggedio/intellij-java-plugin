package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.ParameterAdapter;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.ui.IOTreeCellRenderer;
import com.insidious.plugin.ui.MethodExecutionListener;
import com.insidious.plugin.util.TestCandidateUtils;
import com.insidious.plugin.util.UIUtils;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class TestCandidateListedItemComponent {
    private final TestCandidateMetadata candidateMetadata;
    private final MethodAdapter method;
    private final List<String> methodArgumentValues;
    private final MethodExecutionListener listener;
    private final Map<String, String> parameterMap;
    private JPanel mainPanel;
    private JLabel statusLabel;
    private JPanel mainContentPanel;
    private JLabel executeLabel;
    private JPanel controlPanel;
    private AgentResponseComponent responseComponent;

    public TestCandidateListedItemComponent(TestCandidateMetadata candidateMetadata, MethodAdapter method,
                                            MethodExecutionListener listener) {
        this.candidateMetadata = candidateMetadata;
        this.method = method;
        this.listener = listener;
        this.methodArgumentValues = TestCandidateUtils.buildArgumentValuesFromTestCandidate(
                candidateMetadata);
        this.parameterMap = generateParameterMap();

        loadInputTree();
        executeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                executeCandidate();
            }
        });
        statusLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                displayResponse();
            }
        });
        executeLabel.setIcon(UIUtils.EXECUTE_COMPONENT);
        statusLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        executeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//        mainPanel.setSize(new Dimension(-1, Math.max(150, 45 * method.getParameters().length)));
        mainPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                displayResponse();
            }
        });
    }

    public JPanel getComponent() {
        return this.mainPanel;
    }

    public TestCandidateMetadata getCandidateMetadata() {
        return candidateMetadata;
    }

    private void displayResponse() {
        if (this.responseComponent != null) {
            this.listener.displayResponse(this.responseComponent);
        }
    }

    private void displayExceptionResponse(AgentExceptionResponseComponent comp) {
        this.listener.displayExceptionResponse(comp);
    }

    private void loadInputTree() {
        this.mainContentPanel.removeAll();
        DefaultMutableTreeNode inputRoot = new DefaultMutableTreeNode("");
        Set<String> methodArgumentNames = this.parameterMap.keySet();
        int methodArgumentCount = methodArgumentNames.size();
        if (methodArgumentCount == 0) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode("No inputs for this method.");
            inputRoot.add(node);
        } else {
            for (String key : methodArgumentNames) {
                DefaultMutableTreeNode node = buildJsonTree(this.parameterMap.get(key), key);
                inputRoot.add(node);
            }
        }

//        GridConstraints constraints = new GridConstraints();
//        constraints.setRow(1);
        JTree inputTree = new Tree(inputRoot);
        inputTree.setBorder(JBUI.Borders.empty());

//        for(int i = inputTree.getRowCount() - 1; i >= 0; i--){
//            inputTree.collapseRow(i);
//        }


        inputTree.setCellRenderer(new IOTreeCellRenderer());
        inputTree.setRootVisible(false);
        inputTree.setShowsRootHandles(true);

        GridLayout gridLayout = new GridLayout(1, 1);
        int desiredHeightPerInput = 30;
        int desiredHeight = Math.min(inputRoot.getLeafCount() * desiredHeightPerInput, 200);
        Dimension preferredSize = new Dimension(-1, desiredHeight);

//        mainContentPanel.setPreferredSize(preferredSize);
//        mainContentPanel.setMaximumSize(preferredSize);
//        gridLayout.preferredLayoutSize(mainContentPanel);


        inputTree.setSize(new Dimension(-1, desiredHeight));
        inputTree.setMaximumSize(new Dimension(-1, desiredHeight));

        JScrollPane scrollPane = new JBScrollPane(inputTree);
        scrollPane.setBorder(JBUI.Borders.empty());

        scrollPane.setPreferredSize(new Dimension(-1, desiredHeight));
        scrollPane.setMaximumSize(new Dimension(-1, desiredHeight));
        scrollPane.setSize(new Dimension(-1, desiredHeight));


        mainPanel.setPreferredSize(preferredSize);
        mainPanel.setMaximumSize(preferredSize);

        mainContentPanel.setLayout(gridLayout);
        mainContentPanel.setSize(new Dimension(-1, desiredHeight));

        mainContentPanel.setMaximumSize(preferredSize);
        mainContentPanel.setPreferredSize(preferredSize);
        mainContentPanel.add(scrollPane);

        mainContentPanel.revalidate();
        mainContentPanel.repaint();
    }

    private DefaultMutableTreeNode buildJsonTree(String source, String name) {
        if (source.startsWith("{")) {
            return handleObject(new JSONObject(source), new DefaultMutableTreeNode(name));
        } else if (source.startsWith("[")) {
            return handleArray(new JSONArray(source), new DefaultMutableTreeNode(name));
        } else {
            return new DefaultMutableTreeNode(name + " = " + source);
        }
    }

    private DefaultMutableTreeNode handleObject(JSONObject json, DefaultMutableTreeNode root) {
        Set<String> keys = json.keySet();
        for (String key : keys) {
            String valueTemp = json.get(key)
                    .toString();
            if (valueTemp.startsWith("{")) {
                //obj in obj
                DefaultMutableTreeNode thisKey = new DefaultMutableTreeNode(key);
                JSONObject subObj = new JSONObject(valueTemp);
                handleObject(subObj, thisKey);
                root.add(thisKey);
            } else if (valueTemp.startsWith("[")) {
                //list
                DefaultMutableTreeNode thisKey = new DefaultMutableTreeNode(key);
                JSONArray subObjArray = new JSONArray(valueTemp);
                handleArray(subObjArray, thisKey);
                root.add(thisKey);
            } else {
                DefaultMutableTreeNode thisKVpair = new DefaultMutableTreeNode(key + " : " + valueTemp);
                root.add(thisKVpair);
            }
        }
        return root;
    }

    private DefaultMutableTreeNode handleArray(JSONArray json, DefaultMutableTreeNode root) {
        for (int i = 0; i < json.length(); i++) {
            String valueTemp = json.get(i)
                    .toString();
            if (valueTemp.startsWith("{")) {
                //obj in obj
                DefaultMutableTreeNode thisKey = new DefaultMutableTreeNode(i + " : ");
                JSONObject subObj = new JSONObject(valueTemp);
                handleObject(subObj, thisKey);
                root.add(thisKey);
            } else {
                DefaultMutableTreeNode thisKVpair = new DefaultMutableTreeNode(i + " : " + valueTemp);
                root.add(thisKVpair);
            }
        }
        return root;
    }

    public Map<String, String> getParameterMap() {
        if (this.parameterMap == null || this.parameterMap.size() == 0) {
            return generateParameterMap();
        } else {
            return this.parameterMap;
        }
    }

    public Map<String, String> generateParameterMap() {
        ParameterAdapter[] parameters = method.getParameters();
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

    public List<String> getMethodArgumentValues() {
        return methodArgumentValues;
    }

    public void executeCandidate() {
        JSONObject eventProperties = new JSONObject();
        eventProperties.put("className", candidateMetadata.getTestSubject().getType());
        eventProperties.put("methodName", candidateMetadata.getMainMethod().getMethodName());
        UsageInsightTracker.getInstance().RecordEvent("REXECUTE_SINGLE", eventProperties);
        this.listener.executeCandidate(this.candidateMetadata, this);
    }

    public void setResponseComponent(AgentResponseComponent responseComponent) {
        this.responseComponent = responseComponent;
        Boolean status = this.responseComponent.computeDifferences();
        if (status == null) {
            this.statusLabel.setText("Exception");
            this.statusLabel.setForeground(UIUtils.orange);
            this.statusLabel.setIcon(UIUtils.ORANGE_EXCEPTION);
            return;
        }
        if (!status) {
            //no diff
            this.statusLabel.setText("Same");
            this.statusLabel.setForeground(UIUtils.green);
            this.statusLabel.setIcon(UIUtils.NO_DIFF_GUTTER);
        } else {
            //diff
            this.statusLabel.setText("Different");
            this.statusLabel.setForeground(UIUtils.red);
            this.statusLabel.setIcon(UIUtils.DIFF_GUTTER);
        }
    }

    public void setAndDisplayResponse(AgentResponseComponent responseComponent) {
        this.responseComponent = responseComponent;
        Boolean status = this.responseComponent.computeDifferences();
        if (status == null) {
            this.statusLabel.setText("Exception");
            this.statusLabel.setForeground(UIUtils.orange);
            this.statusLabel.setIcon(UIUtils.ORANGE_EXCEPTION);
            System.out.println("Exception message : " + this.responseComponent
                    .getAgentCommandResponse().getMessage());
            displayResponse();
            return;
        }
        if (!status) {
            //no diff
            this.statusLabel.setText("Same");
            this.statusLabel.setForeground(UIUtils.green);
            this.statusLabel.setIcon(UIUtils.NO_DIFF_GUTTER);
        } else {
            //diff
            this.statusLabel.setText("Different");
            this.statusLabel.setForeground(UIUtils.red);
            this.statusLabel.setIcon(UIUtils.DIFF_GUTTER);
        }
        displayResponse();
    }

    public void DisplayExceptionResponse(AgentExceptionResponseComponent exceptionResponseComponent) {
        this.statusLabel.setText("Exception");
        this.statusLabel.setIcon(UIUtils.ORANGE_EXCEPTION);
        this.statusLabel.setForeground(UIUtils.orange);
        displayExceptionResponse(exceptionResponseComponent);
    }

    public int getHash() {
        int hash = -1;
        if (this.candidateMetadata != null && this.methodArgumentValues != null) {
            String output = new String(
                    this.candidateMetadata.getMainMethod().getReturnDataEvent().getSerializedValue());
            String concat = this.methodArgumentValues.toString() + output;
            hash = concat.toString().hashCode();
        }
        return hash;
    }

}
