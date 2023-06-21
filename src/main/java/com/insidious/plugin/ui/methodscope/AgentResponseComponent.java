package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.datafile.AtomicRecordService;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.ui.Components.AtomicRecord.AtomicRecordListener;
import com.insidious.plugin.ui.Components.ResponseMapTable;
import com.insidious.plugin.ui.Components.AtomicRecord.SaveForm;
import com.insidious.plugin.util.*;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

public class AgentResponseComponent implements Supplier<Component>, AtomicRecordListener {
    private static final Logger logger = LoggerUtil.getInstance(AgentResponseComponent.class);
    private static final boolean SHOW_TEST_CASE_CREATE_BUTTON = true;
    private final AgentCommandResponse<String> agentCommandResponse;
    private final boolean MOCK_MODE = false;
    final private boolean showAcceptButton;
    String s1 = "{\"indicate\":[{\"name\":\"c\",\"age\":24},\"doing\",\"brain\"],\"thousand\":false,\"number\":\"machine\",\"wut\":\"ay\",\"get\":\"ay\",\"sut\":\"ay\",\"put\":\"ay\",\"fut\":\"ay\"}";
    //    String s1 = "";
    String s2 = "{\"indicate\":[{\"name\":\"a\",\"age\":25},\"doing\",\"e\"],\"thousand\":false,\"number\":\"dababy\",\"e\":\"f\"}";
    private JPanel mainPanel;
    private JPanel centerPanel;
    private JPanel bottomControlPanel;
    private JButton viewFullButton;
    private JTable mainTable;
    private JLabel statusLabel;
    private JPanel tableParent;
    private JPanel methodArgumentsPanel;
    private JButton acceptButton;
    private JScrollPane scrollParent;
    private JPanel topPanel;
    private JPanel topAlign;
    private JButton deleteButton;
    private JButton hideButton;
    private StoredCandidate metadata;
    private SaveForm lastRef;
    private AgentResponseComponent self = this;
    private String methodHash;
    private String methodSignature;
    private AtomicRecordService atomicRecordService;
    private String classname;
    private String methodName;

    public AgentResponseComponent(
            AgentCommandResponse<String> agentCommandResponse,
            StoredCandidate metadata,
            boolean showAcceptButton,
            FullViewEventListener fullViewEventListener,
            AtomicRecordService atomicRecordService
    ) {
        this.agentCommandResponse = agentCommandResponse;
        this.showAcceptButton = showAcceptButton;
        this.metadata = metadata;
        this.atomicRecordService = atomicRecordService;

        if (!showAcceptButton) {
            this.bottomControlPanel.setVisible(false);
        }

        DifferenceResult differences = DiffUtils.calculateDifferences(metadata, agentCommandResponse);
        computeDifferences(differences);


        byte[] serializedValue = metadata.getReturnDataEventSerializedValue().getBytes();
        String originalString = serializedValue.length > 0 ? new String(serializedValue) :
                String.valueOf(metadata.getReturnDataEventValue());
        String actualString = String.valueOf(agentCommandResponse.getMethodReturnValue());

        String simpleClassName = agentCommandResponse.getTargetClassName();
        simpleClassName = simpleClassName.substring(simpleClassName.lastIndexOf(".") + 1);


        String methodLabel = simpleClassName + "." + agentCommandResponse.getTargetMethodName() + "()";

        setInfoLabel(methodLabel + " at " + DateUtils.formatDate(new Date(agentCommandResponse.getTimestamp())));
        viewFullButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (MOCK_MODE) {
//                    GenerateCompareWindows(s1, s2);
                    fullViewEventListener.generateCompareWindows(s1, s2);
                } else {
                    fullViewEventListener.generateCompareWindows(originalString, actualString);
                }
            }
        });

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
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                InsidiousNotification.notifyMessage("Removing this candidate. ",
                        NotificationType.INFORMATION);
            }
        });
        acceptButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(lastRef!=null)
                {
                    lastRef.dispose();
                }
                lastRef = new SaveForm(self);
                lastRef.setStoredCandidate(metadata);
                lastRef.setVisible(true);
            }
        });
        if(metadata.getCandidateId()==null)
        {
            deleteButton.setVisible(false);
        }
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(metadata.getCandidateId()!=null)
                {
                    atomicRecordService.deleteStoredCandidate(classname,
                            methodName+"#"+methodSignature, metadata.getCandidateId());
                }
            }
        });
    }

    public void setInfoLabel(String info) {
        TitledBorder titledBorder = (TitledBorder) mainPanel.getBorder();
        titledBorder.setTitle(info);
//        this.infoLabel.setText(info);
    }

    public void computeDifferences(DifferenceResult differenceResult) {

        switch (differenceResult.getDiffResultType()) {
            case DIFF:
                this.statusLabel.setText("Differences Found.");
                this.statusLabel.setIcon(UIUtils.DIFF_GUTTER);
                this.statusLabel.setForeground(UIUtils.red);
                renderTableWithDifferences(differenceResult.getDifferenceInstanceList());
                break;
            case NO_ORIGINAL:
                this.statusLabel.setText("No previous Candidate found, current response.");
                renderTableForResponse(differenceResult.getRightOnly());
                break;
            case SAME:
                this.statusLabel.setText("Both Responses are Equal.");
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
                                this.metadata.getReturnDataEventSerializedValue())
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
        ResponseMapTable newModel = new ResponseMapTable(rightOnly);
        this.mainTable.setModel(newModel);
        this.mainTable.revalidate();
        this.mainTable.repaint();
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

    public void showExceptionTrace(String response) {
        this.tableParent.removeAll();
        JTextArea textArea = new JTextArea();
        textArea.setText(response);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
//        JScrollPane scroll = new JBScrollPane(textArea,
//                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        this.tableParent.add(textArea, BorderLayout.CENTER);
        this.tableParent.revalidate();
    }

    @Override
    public Component get() {
        return this.mainPanel;
    }

    @Override
    public void triggerRecordAddition(String name, String description, StoredCandidate.AssertionType type) {
        this.metadata.setMethodHash(methodHash);
        StoredCandidate candidate = AtomicRecordUtils.createCandidateFor(metadata,agentCommandResponse);
        candidate.setName(name);
        candidate.setDescription(description);
        candidate.setAssertionType(type);
        atomicRecordService.saveCandidate(classname,methodName,methodSignature,candidate);
    }

    @Override
    public void deleteCandidateRecord() {
    }

    @Override
    public String getSaveLocation() {
        return atomicRecordService.getSaveLocation();
    }

    public void setMethodName(String name) {this.methodName = name;}
    public void setMethodHash(String methodHash) {
        this.methodHash = methodHash;
    }

    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }

    public void setClassname(String qualifiedName) {
        this.classname = qualifiedName;
    }
}
