package com.insidious.plugin.ui;

import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.testcase.TestCaseService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.intellij.notification.NotificationType;
import com.intellij.uiDesigner.core.GridConstraints;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Set;

public class CandidateInformationWindow implements TreeSelectionListener, TestSelectionListener {
    private JPanel mainPanel;
    private JSplitPane mainSplitPane;
    private JPanel candidateListParent;
    private JPanel IOsectionParent;
    private JPanel IOgroup;
    private JPanel outputSP;
    private JPanel inputSP;
    private JButton assertEqualsBtn;
    private JPanel buttonGroup;
    private JButton assertNotEqualsBtn;
    private JPanel inputPanel;
    private JPanel outputPanel;
    private JLabel inputLabelHeader;
    private JLabel outputLabelHeader;
    private TestSelectionListener candidateSelectionListener;
    private TestCandidateMetadata lastSelectedCandidate;

    private IOTreeCellRenderer ioTreeCellRenderer = new IOTreeCellRenderer();

    public CandidateInformationWindow(
            List<TestCandidateMetadata> testCandidateMetadataList,
            TestCaseService testCaseService,
            TestSelectionListener candidateSelectionListener,
            SessionInstance sessionInstance
    ) {
        this.candidateSelectionListener = candidateSelectionListener;
        constructCandidatesList(testCandidateMetadataList, testCaseService, sessionInstance);
        assertEqualsBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                if (lastSelectedCandidate != null) {
                    onSelect(lastSelectedCandidate);
                } else {
                    InsidiousNotification.notifyMessage("Please select a candidate first.",
                            NotificationType.ERROR);
                }
            }
        });

    }

    public void constructCandidatesList(List<TestCandidateMetadata> testCandidateMetadataList, TestCaseService testCaseService, SessionInstance sessionInstance) {
        this.candidateListParent.removeAll();
        int GridRows = 6;
        if (testCandidateMetadataList.size() > GridRows) {
            GridRows = testCandidateMetadataList.size();
        }
        GridLayout gridLayout = new GridLayout(GridRows, 1);
        gridLayout.setVgap(8);
        JPanel gridPanel = new JPanel(gridLayout);
        for (int i = 0; i < testCandidateMetadataList.size(); i++) {
            TestCandidateMetadata testCandidateMetadata = testCandidateMetadataList.get(i);
            GridConstraints constraints = new GridConstraints();
            constraints.setRow(i);
            TestCandidateMetadataView testCandidatePreviewPanel = new TestCandidateMetadataView(
                    testCandidateMetadata, testCaseService, this, sessionInstance);
            if (i == 0) {
                testCandidatePreviewPanel.setSelectedState(true);
            }
            testCandidatePreviewPanel.setCandidateNumberIndex((i + 1));
            Component contentPanel = testCandidatePreviewPanel.getContentPanel();
            gridPanel.add(contentPanel, constraints);
        }

        JScrollPane scrollPane = new JScrollPane(gridPanel);
        candidateListParent.setPreferredSize(scrollPane.getSize());
        candidateListParent.add(scrollPane, BorderLayout.CENTER);
        if (testCandidateMetadataList.size() <= 4) {
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        }

        if (testCandidateMetadataList.size() > 0) {
            loadInputOutputInformation(testCandidateMetadataList.get(0));
        }
        this.candidateListParent.revalidate();
    }

    public JPanel getMainPanel() {
        return this.mainPanel;
    }

    @Override
    public void onSelect(TestCandidateMetadata testCandidateMetadata) {
        lastSelectedCandidate = testCandidateMetadata;
        this.candidateSelectionListener.onSelect(testCandidateMetadata);
    }

    @Override
    public void loadInputOutputInformation(TestCandidateMetadata testCandidateMetadata) {
        lastSelectedCandidate = testCandidateMetadata;
        MethodCallExpression exp = (MethodCallExpression) testCandidateMetadata.getMainMethod();
        List<Parameter> args = exp.getArguments();
        DefaultMutableTreeNode inputRoot = new DefaultMutableTreeNode("");
        for (Parameter arg : args) {
            inputRoot.add(constructNode(arg));
        }
        Parameter returnValue = exp.getReturnValue();
        DefaultMutableTreeNode outputroot = constructNode(returnValue);
        try {
            inputSP.removeAll();
            outputSP.removeAll();

            this.inputSP.setLayout(new GridLayout(1, 1));
            GridConstraints constraints = new GridConstraints();
            constraints.setRow(1);
            JTree inputTree = new JTree(inputRoot);
            JScrollPane scrollPane = new JScrollPane(inputTree);
            this.inputSP.setPreferredSize(scrollPane.getSize());
            this.inputSP.add(scrollPane, BorderLayout.CENTER);
            inputTree.setCellRenderer(ioTreeCellRenderer);
            inputTree.setRootVisible(false);
            inputTree.setShowsRootHandles(true);
            this.inputSP.revalidate();


            this.outputSP.setLayout(new GridLayout(1, 1));
            GridConstraints constraints1 = new GridConstraints();
            constraints1.setRow(1);
            JTree outputTree = new JTree(outputroot);
            JScrollPane scrollPane1 = new JScrollPane(outputTree);
            this.outputSP.setPreferredSize(scrollPane1.getSize());
            this.outputSP.add(scrollPane1, BorderLayout.CENTER);
            outputTree.setCellRenderer(ioTreeCellRenderer);
            this.outputSP.revalidate();
        } catch (Exception e) {
            System.out.println("Exception generation IO tree");
            e.printStackTrace();
        }
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {

    }

    private DefaultMutableTreeNode constructNode(Parameter arg) {
        StringBuilder sb = new StringBuilder();
        if(arg==null)
        {
            return null;
        }
        switch (arg.getType()) {
            case "I":
                String name = "";
                if (arg.getName() != null) {
                    name = arg.getName();
                }
                return new DefaultMutableTreeNode(new ParameterInformation("int", arg.getName(), "" + arg.getValue(), true));
            case "J":
                name = "";
                if (arg.getName() != null) {
                    name = arg.getName();
                }
                return new DefaultMutableTreeNode(new ParameterInformation("long", arg.getName(), "" + arg.getValue(), true));
            case "Z":
                name = "";
                if (arg.getName() != null) {
                    name = arg.getName();
                }
                String booleanValue = (arg.getValue() == 0 ? false : true) + "";
                return new DefaultMutableTreeNode(new ParameterInformation("boolean", arg.getName(), booleanValue, true));

            default:
                String serializedValue = new String(arg.getProb().getSerializedValue());
                if (serializedValue != null) {
                    return buildJsonTree(serializedValue, arg);
                } else {
                    return new DefaultMutableTreeNode(new ParameterInformation(getSimpleType(arg.getType()), arg.getName(), "null", true));
                }
        }
    }

    private DefaultMutableTreeNode buildJsonTree(String source, Parameter parameter) {
        if (source.startsWith("{")) {
            return handleObject(new JSONObject(source), new DefaultMutableTreeNode(new ParameterInformation(getSimpleType(parameter.getType()), parameter.getName(), "", false)));
        } else if (source.startsWith("[")) {
            return handleArray(new JSONArray(source), new DefaultMutableTreeNode(new ParameterInformation(getSimpleType(parameter.getType()), parameter.getName(), "", false)));
        } else {
            return new DefaultMutableTreeNode(new ParameterInformation(getSimpleType(parameter.getType()), parameter.getName(), source, true));
        }
    }

    private DefaultMutableTreeNode handleObject(JSONObject json, DefaultMutableTreeNode root) {
        Set<String> keys = json.keySet();
        for (String key : keys) {
            String valueTemp = json.get(key).toString();
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
            String valueTemp = json.get(i).toString();
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

    private String getSimpleType(String type) {
        String[] parts = type.split("\\.");
        if (parts.length > 0) {
            return parts[parts.length - 1];

        }
        return type;
    }

    class ParameterInformation {
        public String type;
        public String name;
        public String value;
        boolean showValue = false;

        public ParameterInformation(String type, String name, String value, boolean showValue) {
            this.type = type;
            this.name = name;
            this.value = value;
            this.showValue = showValue;
        }

        @Override
        public String toString() {
            return type + " " + ((name == null) ? "" : name) + ((showValue) ? " = " + ((showValue) ? value : "") : "");
        }
    }
}
