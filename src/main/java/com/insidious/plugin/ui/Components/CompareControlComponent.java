package com.insidious.plugin.ui.Components;

import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.ui.IOTreeCellRenderer;
import com.insidious.plugin.ui.MethodExecutionListener;
import com.insidious.plugin.ui.UIUtils;
import com.insidious.plugin.ui.adapter.MethodAdapter;
import com.insidious.plugin.ui.adapter.ParameterAdapter;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.ui.JBUI;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class CompareControlComponent {
    String s1 = "{\"indicate\":[{\"name\":\"c\",\"age\":24},\"doing\",\"brain\"],\"thousand\":false,\"number\":\"machine\",\"wut\":\"ay\",\"get\":\"ay\",\"sut\":\"ay\",\"put\":\"ay\",\"fut\":\"ay\"}";
    //    String s1 = "";
    String d1 = "1";
    String s2 = "{\"indicate\":[{\"name\":\"a\",\"age\":25},\"doing\",\"e\"],\"thousand\":false,\"number\":\"dababy\",\"e\":\"f\"}";
    //    String s1 = "{\"indicate\":[{\"name\":\"a\",\"age\":25},\"doing\",\"e\"],\"thousand\":false,\"number\":\"daboi\"}";
    String d2 = "1";
    private JPanel mainPanel;
    private JPanel borderParent;
    private JPanel controlPanel;
    private JLabel statusLabel;
    private JPanel mainContentPanel;
    private JLabel executeLabel;
    private JPanel gridParent;
    private Map<String, String> parameterMap;
    private TestCandidateMetadata candidateMetadata;
    private MethodAdapter method;
    private List<String> methodArgumentValues;
    private MethodExecutionListener listener;
    private AgentResponseComponent responseComponent;

    public CompareControlComponent(TestCandidateMetadata candidateMetadata, List<String> methodArgumentValues,
                                   MethodAdapter method,
                                   MethodExecutionListener listener) {
        this.candidateMetadata = candidateMetadata;
        this.method = method;
        this.listener = listener;
        this.methodArgumentValues = methodArgumentValues;
        if (candidateMetadata == null) {
            return;
        }
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
        borderParent.addMouseListener(new MouseAdapter() {
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

    private void loadInputTree() {
        this.mainContentPanel.removeAll();
        DefaultMutableTreeNode inputRoot = new DefaultMutableTreeNode("");
        if(this.parameterMap.keySet()==null || this.parameterMap.keySet().size()==0)
        {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode("No inputs for this method.");
            inputRoot.add(node);
        }
        else {
            for (String key : this.parameterMap.keySet()) {
                DefaultMutableTreeNode node = buildJsonTree(this.parameterMap.get(key), key);
                inputRoot.add(node);
            }
        }

        this.mainContentPanel.setLayout(new GridLayout(1, 1));
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(1);
        JTree inputTree = new Tree(inputRoot);
        inputTree.setBorder(JBUI.Borders.empty());
        JScrollPane scrollPane = new JBScrollPane(inputTree);
        scrollPane.setBorder(new EtchedBorder());
        this.mainContentPanel.setPreferredSize(scrollPane.getSize());
        scrollPane.setBorder(JBUI.Borders.empty());
        this.mainContentPanel.add(scrollPane, BorderLayout.CENTER);
        inputTree.setCellRenderer(new IOTreeCellRenderer());
        inputTree.setRootVisible(false);
        inputTree.setShowsRootHandles(true);
        this.mainContentPanel.revalidate();
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
                String parameterValue = methodArgumentValues == null ? "" : methodArgumentValues.get(i);
                parameterInputMap.put(methodParameter.getName(), parameterValue);
            }
        }
        return parameterInputMap;
    }

    public List<String> getMethodArgumentValues() {
        return methodArgumentValues;
    }

    public void executeCandidate() {
        this.listener.executeCandidate(this.candidateMetadata, this);
    }

    public void setResposeComponent(AgentResponseComponent responseComponent) {
        this.responseComponent = responseComponent;
        Boolean status = this.responseComponent.computeDifferences();
        if(status==null)
        {
            this.statusLabel.setText("Exception");
            this.statusLabel.setIcon(UIUtils.EXCEPTION_CASE);
            return;
        }
        if (!status) {
            //no diff
            this.statusLabel.setText("Same");
            this.statusLabel.setIcon(UIUtils.NO_DIFF_GUTTER);
        } else {
            //diff
            this.statusLabel.setText("Different");
            this.statusLabel.setIcon(UIUtils.DIFF_GUTTER);
        }
    }

    public void setAndDisplayResponse(AgentResponseComponent responseComponent) {
        this.responseComponent = responseComponent;
        Boolean status = this.responseComponent.computeDifferences();
        if(status==null)
        {
            this.statusLabel.setText("Exception");
            this.statusLabel.setIcon(UIUtils.EXCEPTION_CASE);
            System.out.println("Exception message : "+this.responseComponent
                    .getAgentCommandResponse().getMessage());
            displayResponse();
            return;
        }
        if (!status) {
            //no diff
            this.statusLabel.setText("Same");
            this.statusLabel.setIcon(UIUtils.NO_DIFF_GUTTER);
        } else {
            //diff
            this.statusLabel.setText("Different");
            this.statusLabel.setIcon(UIUtils.DIFF_GUTTER);
        }
        displayResponse();
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

    public Map<String, String> generateMockParameterMap() {
        Map<String, String> map = new TreeMap<>();
        map.put("val1", s1);
        map.put("val2", s2);
        map.put("val3", d1);
        map.put("val4", d2);
        return map;
    }
}