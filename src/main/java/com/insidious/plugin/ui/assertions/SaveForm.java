package com.insidious.plugin.ui.assertions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.assertions.*;
import com.insidious.plugin.callbacks.CandidateLifeListener;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.util.JsonTreeUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import org.json.JSONObject;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.List;

import com.insidious.plugin.atomicrecord.AtomicRecordService;
import static com.insidious.plugin.util.ParameterUtils.processResponseForFloatAndDoubleTypes;

public class SaveForm {

    private static final Logger logger = LoggerUtil.getInstance(SaveForm.class);
    private final static ObjectMapper objectMapper = new ObjectMapper();
    private final CandidateLifeListener listener;
    private final StoredCandidate storedCandidate;
    private final AgentCommandResponse<String> agentCommandResponse;
    private final AssertionBlock ruleEditor;
    private final SaveFormMetadataPanel metadataForm;
    private final JPanel mainPanel;
    private HashSet<DeclaredMock> enabledMockList;
    private JsonNode responseNode;
    private JButton saveButton;
    // private JButton cancelButton;
    private JTree candidateExplorerTree;
    private InsidiousService insidiousService;
    //AgentCommandResponse is necessary for update flow and Assertions as well
    private HashMap<JCheckBox, ArrayList<JCheckBox>> buttonMap = new HashMap<JCheckBox, ArrayList<JCheckBox>>();

    public SaveForm(
            StoredCandidate storedCandidate,
            AgentCommandResponse<String> agentCommandResponse,
            CandidateLifeListener listener
    ) {
        this.storedCandidate = storedCandidate;
        this.listener = listener;
        agentCommandResponse.setMethodReturnValue(processResponseForFloatAndDoubleTypes(agentCommandResponse.getResponseClassName(),
                agentCommandResponse.getMethodReturnValue()));
        this.agentCommandResponse = agentCommandResponse;
        this.enabledMockList = new HashSet<>(this.storedCandidate.getEnabledMock());

        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        candidateExplorerTree = new Tree(getTree());

        String methodReturnValue = agentCommandResponse.getMethodReturnValue();

        responseNode = getResponseNode(methodReturnValue, agentCommandResponse.getResponseClassName());


        // clone the assertions
        AtomicAssertion existingAssertion = new AtomicAssertion(storedCandidate.getTestAssertions());

        if (existingAssertion.getSubAssertions() == null || existingAssertion.getSubAssertions().size() == 0) {
            List<AtomicAssertion> subAssertions = new ArrayList<>();
            subAssertions.add(
                    new AtomicAssertion(AssertionType.EQUAL, "/", methodReturnValue));
            existingAssertion = new AtomicAssertion(AssertionType.ALLOF, subAssertions);
        }
        ruleEditor = new AssertionBlock(existingAssertion, new AssertionBlockManager() {
            @Override
            public void addNewRule() {

            }

            @Override
            public void addNewGroup() {

            }

            @Override
            public AssertionResult executeAssertion(AtomicAssertion atomicAssertion) {
                return AssertionEngine.executeAssertions(atomicAssertion, responseNode);
            }

            @Override
            public void deleteAssertionRule(AssertionRule element) {

            }

            @Override
            public void removeAssertionGroup() {
                InsidiousNotification.notifyMessage("Cannot delete this group.", NotificationType.ERROR);
            }

            @Override
            public KeyValue getCurrentTreeKey() {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) candidateExplorerTree.getLastSelectedPathComponent();
                String methodReturnValue = agentCommandResponse.getMethodReturnValue();
                if (node == null) return new KeyValue("/", methodReturnValue);
                TreeNode[] nodes = node.getPath();
                String selectedKey = JsonTreeUtils.getFlatMap(nodes);
                Object valueFromJsonNode = JsonTreeUtils.getValueFromJsonNode(responseNode, selectedKey);
                return new KeyValue(selectedKey, valueFromJsonNode);
            }

            @Override
            public void removeAssertionGroup(AssertionBlock block) {

            }
        }, true);

        int panelWidth = 492;
        int lowerPanelWidth = panelWidth * 2; // lowerPanelWidth -> doublePanelWidth
        int lowerPanelHeight = 296;
        JPanel ruleEditor = this.ruleEditor.getMainPanel();

        // define topPanel
        JPanel topPanel = new JPanel();
        GridLayout topPanelLayout = new GridLayout(1, 2);
        topPanel.setLayout(topPanelLayout);

        // define saveTestCaseHeading
        JLabel saveTestCaseHeading = new JLabel();
        saveTestCaseHeading.setIcon(UIUtils.TESTTUBE);
        saveTestCaseHeading.setText("<html><b>Save Test Case</b></html>");
        saveTestCaseHeading.setBorder(JBUI.Borders.empty(10));

        // define saveTestCasePanel
        JPanel saveTestCasePanel = new JPanel();
        saveTestCasePanel.add(saveTestCaseHeading);
        saveTestCasePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(saveTestCasePanel);

        // define the buttonPanel
        JPanel buttonPanel = new JPanel();

        // cancel button in panel
        JButton cancelButton = new JButton();
        cancelButton.setText("Cancel");
        cancelButton.addActionListener(e -> listener.onCancel());
        buttonPanel.add(cancelButton);

        // save button in panel
        this.saveButton = new JButton();
        this.saveButton.setIcon(UIUtils.SAVE_CANDIDATE_GREEN_SVG);
        this.saveButton.setText("Save & Close");
        this.saveButton.addActionListener(e -> triggerSave());
        buttonPanel.add(this.saveButton);

        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        topPanel.add(buttonPanel);

        JPanel midPanel = new JPanel();
        midPanel.setLayout(new GridLayout(1, 2));

        // define treePanelHeading
        JLabel treePanelHeading = new JLabel();
        treePanelHeading.setText("<html><b>Available recorded objects:</b></html>");
        treePanelHeading.setVerticalAlignment(SwingConstants.TOP);
        treePanelHeading.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        // define treePanel
        JPanel treePanel = new JPanel();
        treePanel.setLayout(new BorderLayout());
        treePanel.add(treePanelHeading, BorderLayout.NORTH);

        int CandidateExplorerTreeHeight = 300;

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        mainPanel.setPreferredSize(new Dimension(-1,
                (int) (screenSize.getHeight() - (0.3 * screenSize.getHeight()))));
        candidateExplorerTree.setSize(new Dimension(400, CandidateExplorerTreeHeight));
        JScrollPane treeParent = new JBScrollPane(candidateExplorerTree);
        treePanel.add(treeParent, BorderLayout.CENTER);
        treePanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 0, new Color(50, 50, 50)));

        // define the metadataPanel
        metadataForm = new SaveFormMetadataPanel(new MetadataViewPayload(storedCandidate.getName(),
                storedCandidate.getDescription(),
                storedCandidate.getMetadata()));

        JPanel metadataFormPanel = metadataForm.getMainPanel();
        metadataFormPanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(50, 50, 50)));

        midPanel.add(treePanel);
        midPanel.add(metadataFormPanel);

        // assertion panel
        JPanel assertionPanel = new JPanel();
        GridLayout assertionPanelLayout = new GridLayout(1, 1);
        assertionPanel.setLayout(assertionPanelLayout);

        JScrollPane assertionScrollPanel = new JBScrollPane(ruleEditor);
        assertionPanel.add(assertionScrollPanel);

        // mock panel
        JPanel mockPanel = new JPanel();
        GridLayout mockPanelLayout = new GridLayout(1, 1);
        mockPanel.setLayout(mockPanelLayout);

        // define mockDataPanelContent
        JPanel mockDataPanelContent = new JPanel();
        mockDataPanelContent.setLayout(new BoxLayout(mockDataPanelContent, BoxLayout.Y_AXIS));

        // define applyMockLabel
        JLabel applyMockLabel = new JLabel();
        applyMockLabel.setText("<html><b>Apply Mock</b></html>");
        JPanel applyMockPanel = new JPanel();
        applyMockPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        applyMockPanel.add(applyMockLabel);
        applyMockPanel.setMaximumSize(new Dimension(3999, 30));
        mockDataPanelContent.add(applyMockPanel);
        int mockDataPanelContentHeight = 0;

        this.insidiousService = this.listener.getProject().getService(InsidiousService.class);
        AtomicRecordService atomicRecordService = this.insidiousService.getAtomicRecordService();

        MethodAdapter temp_method_adapter = this.insidiousService.getCurrentMethod();
        MethodUnderTest temp_method_under_test = MethodUnderTest.fromMethodAdapter(temp_method_adapter);
        System.out.println("temp_method_under_test= " + temp_method_under_test);

        List<DeclaredMock> temp_available_mocks = atomicRecordService.getDeclaredMocksFor(temp_method_under_test);
        HashMap<String, ArrayList<DeclaredMock>> dependency_mock_map = new HashMap<String, ArrayList<DeclaredMock>>();

        for (int i = 0; i <= temp_available_mocks.size() - 1; i++) {
            DeclaredMock local_mock = temp_available_mocks.get(i);
            String local_mock_name = local_mock.getName();
            String local_mock_method_name = local_mock.getMethodName();
            System.out.println("local_mock_name = " + local_mock_name);
            System.out.println("local_mock_method_name = " + local_mock_method_name);
            if (dependency_mock_map.containsKey(local_mock_method_name)) {
                dependency_mock_map.get(local_mock_method_name).add(local_mock);
            } else {
                dependency_mock_map.put(local_mock_method_name, new ArrayList<DeclaredMock>());
                dependency_mock_map.get(local_mock_method_name).add(local_mock);
            }
        }
        System.out.println("dependency_mock_map = " + dependency_mock_map.toString());

        // define mockMethodPanel
        JPanel mockMethodPanel = new JPanel();
        mockMethodPanel.setLayout(new BoxLayout(mockMethodPanel, BoxLayout.Y_AXIS));

        for (String local_key : dependency_mock_map.keySet()) {
            ArrayList<DeclaredMock> local_key_data = dependency_mock_map.get(local_key);
            JPanel mockMethodPanelSingle = new JPanel();
            mockMethodPanelSingle.setLayout(new BoxLayout(mockMethodPanelSingle, BoxLayout.Y_AXIS));
            int mockMethodPanelSingleHeight = 0;

            // define mockMethodNamePanel
            JPanel mockMethodNamePanel = new JPanel();
            GridLayout namePanelLayout = new GridLayout(1, 2);
            mockMethodNamePanel.setLayout(namePanelLayout);

            // define mockMethodNamePanelLeft
            JLabel mockMethodNamePanelLeft = new JLabel();
            mockMethodNamePanelLeft.setText(local_key);
            mockMethodNamePanelLeft.setIcon(UIUtils.MOCK_DATA);
            mockMethodNamePanelLeft.setBorder(JBUI.Borders.empty(4, 8, 4, 0));
            mockMethodNamePanel.add(mockMethodNamePanelLeft);

            // define mockMethodNamePanelRight
            JPanel mockMethodNamePanelRight = new JPanel();
            mockMethodNamePanelRight.setLayout(new FlowLayout(FlowLayout.RIGHT));
            JCheckBox mockButtonMain = new JCheckBox();
            this.buttonMap.put(mockButtonMain, new ArrayList<JCheckBox>());
            mockButtonMain.setSelected(this.enabledMockList != null && this.storedCandidate.getEnabledMock().containsAll(local_key_data));
            ArrayList<JCheckBox> mockButtonMainPart = this.buttonMap.get(mockButtonMain);
            mockButtonMain.addActionListener(e -> {
                if (mockButtonMain.isSelected()) {
                    this.changeAllMocks(dependency_mock_map.get(local_key), true);
                    for (int i = 0; i <= mockButtonMainPart.size() - 1; i++) {
                        mockButtonMainPart.get(i).setSelected(true);
                    }
                } else {
                    this.changeAllMocks(dependency_mock_map.get(local_key), false);
                    for (int i = 0; i <= mockButtonMainPart.size() - 1; i++) {
                        mockButtonMainPart.get(i).setSelected(false);
                    }
                }
            });
            mockMethodNamePanelRight.add(mockButtonMain);
            JLabel selectAllText = new JLabel();
            selectAllText.setText("<html><b>Select all</b></html>");
            mockMethodNamePanelRight.add(selectAllText);
            mockMethodNamePanel.add(mockMethodNamePanelRight);

            mockMethodNamePanel.setMaximumSize(new Dimension(3999, 30));
            mockMethodPanelSingleHeight += 30;
            mockMethodPanelSingle.add(mockMethodNamePanel);

            for (int i = 0; i <= local_key_data.size() - 1; i++) {
                DeclaredMock mockData = local_key_data.get(i);
                // define mockMethodDependencyPanel
                JPanel mockMethodDependencyPanel = new JPanel();
                GridLayout dependencyGrid = new GridLayout(1, 2);
                dependencyGrid.setHgap(8);
                dependencyGrid.setVgap(4);
                mockMethodDependencyPanel.setLayout(dependencyGrid);

                // define mockMethodDependencyPanelLeft
                JPanel mockMethodDependencyPanelLeft = new JPanel();
                JPanel leftText = new JPanel();
                GridLayout twoRowLayout = new GridLayout(2, 1);
                twoRowLayout.setVgap(4);
                twoRowLayout.setHgap(8);
                leftText.setLayout(twoRowLayout);

                JLabel leftTextFirst = new JLabel();
                leftTextFirst.setText(mockData.getName());
                leftText.add(leftTextFirst);

                JLabel leftTextSecond = new JLabel();
                leftTextSecond.setIcon(UIUtils.CLASS_FILE);
                leftTextSecond.setText(mockData.getMethodName());
                leftText.add(leftTextSecond);

                mockMethodDependencyPanelLeft.setLayout(new FlowLayout(FlowLayout.LEFT));
                mockMethodDependencyPanelLeft.add(leftText);
                mockMethodDependencyPanel.add(mockMethodDependencyPanelLeft);

                // define mockMethodDependencyPanelRight
                JPanel mockMethodDependencyPanelRight = new JPanel();
                mockMethodDependencyPanelRight.setLayout(new FlowLayout(FlowLayout.RIGHT));
                JCheckBox mockButton = new JCheckBox();
                mockButton.setSelected(this.enabledMockList != null && this.storedCandidate.getEnabledMock().contains(mockData));
                mockButton.addActionListener(e -> {
                    if (mockButton.isSelected()) {
                        this.changeSingleMock(mockData, true);
                    } else {
                        this.changeSingleMock(mockData, false);
                    }
                });
                mockMethodDependencyPanelRight.add(mockButton);
                this.buttonMap.get(mockButtonMain).add(mockButton);
                mockMethodDependencyPanel.add(mockMethodDependencyPanelRight);

                mockMethodDependencyPanel.setBorder(BorderFactory.createLineBorder(new Color(95, 96, 96)));
                mockMethodDependencyPanel.setMaximumSize(new Dimension(3999, 50));
                mockMethodPanelSingleHeight += 60;
                mockMethodPanelSingle.add(mockMethodDependencyPanel);
            }
            mockMethodPanelSingle.setBorder(BorderFactory.createLineBorder(new Color(50, 50, 50)));
            mockMethodPanelSingleHeight += 20;
            mockMethodPanelSingle.setMaximumSize(new Dimension(3999, mockMethodPanelSingleHeight));
            mockDataPanelContentHeight += mockMethodPanelSingleHeight;
            mockMethodPanel.add(mockMethodPanelSingle);
            mockMethodPanel.add(Box.createRigidArea(null));
        }

        mockDataPanelContent.add(mockMethodPanel);
        JScrollPane mockScrollPanel = new JBScrollPane(mockDataPanelContent);
        mockPanel.add(mockScrollPanel);

        // define lowerPanel
        JTabbedPane bottomControlPanel = new JBTabbedPane();
        bottomControlPanel.addTab("Assertion", assertionPanel);
        bottomControlPanel.addTab("Mock Data", mockPanel);
        bottomControlPanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                JSONObject panelChanged = new JSONObject();
                panelChanged.put("tabIndex", bottomControlPanel.getSelectedIndex());
                UsageInsightTracker.getInstance().RecordEvent("REPEAT_RECORD_TAB_TYPE", panelChanged);
            }
        });
        this.metadataForm.comboBox1.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                System.out.println(e.getSource());

                JSONObject testChange = new JSONObject();
                testChange.put("testType", e.getItem().toString());
                UsageInsightTracker.getInstance().RecordEvent("REPEAT_RECORD_TEST_TYPE", testChange);

                if ((e.getStateChange() == ItemEvent.SELECTED) && (Objects.equals(e.getItem().toString(), "Unit Test"))) {
                    bottomControlPanel.setEnabledAt(1, true);
                    enabledMockList.clear();
                } else {
                    bottomControlPanel.setEnabledAt(1, false);
                    bottomControlPanel.setSelectedIndex(0);
                }
            }
        });
        JPanel primaryContentPanel = new JPanel();
        BoxLayout boxLayout = new BoxLayout(primaryContentPanel, BoxLayout.Y_AXIS);
        primaryContentPanel.setLayout(boxLayout);

        midPanel.setPreferredSize(new Dimension(-1, 320));
        bottomControlPanel.setPreferredSize(new Dimension(-1, 360));

        primaryContentPanel.add(midPanel);
        primaryContentPanel.add(bottomControlPanel);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(primaryContentPanel, BorderLayout.CENTER);
    }

    private void changeAllMocks(List<DeclaredMock> allDeclaredMocks, boolean state) {
        for (int i = 0; i <= allDeclaredMocks.size() - 1; i++) {
            if (state) {
                this.enabledMockList.add(allDeclaredMocks.get(i));
            } else {
                this.enabledMockList.remove(allDeclaredMocks.get(i));
            }
        }
    }

    private void changeSingleMock(DeclaredMock localMock, boolean state) {
        if (state) {
            this.enabledMockList.add(localMock);
        } else {
            this.enabledMockList.remove(localMock);
        }
    }

    private JsonNode getResponseNode(String methodReturnValue, String responseClassName) {
        try {
            return objectMapper.readTree(methodReturnValue);
        } catch (JsonProcessingException e) {
            // this shouldn't happen
            if ("java.lang.String".equals(responseClassName)
                    && !methodReturnValue.startsWith("\"")
                    && !methodReturnValue.endsWith("\"")) {
                try {
                    return objectMapper.readTree("\"" + methodReturnValue + "\"");
                } catch (JsonProcessingException e1) {
                    // failed to read as a json node
                    throw new RuntimeException(e1);
                }
            }
        }
        return null;
    }

    public JPanel getComponent() {
        return mainPanel;
    }

    private void triggerSave() {
        AtomicAssertion atomicAssertion = ruleEditor.getAssertion();

        MetadataViewPayload payload = metadataForm.getPayload();
        String assertionName = prepareString(payload.getName());
        String assertionDescription = prepareString(payload.getDescription());
        AssertionType type = AssertionType.EQUAL;

        // set mocks
        if (this.metadataForm.comboBox1.getSelectedIndex() == 0) {
            // unit test
            ArrayList<DeclaredMock> enabledMockUnDeleted = new ArrayList<>();
            for (DeclaredMock localMock:this.insidiousService.getDeclaredMocksFor(this.storedCandidate.getMethod())) {
                if (this.enabledMockList.contains(localMock)) {
                    enabledMockUnDeleted.add(localMock);
                }
            }
            this.storedCandidate.setEnabledMock(enabledMockUnDeleted);
        }
        else {
            // integration test
            this.storedCandidate.setEnabledMock(new ArrayList<>());
        }
    
        StoredCandidate candidate = StoredCandidate.createCandidateFor(storedCandidate, agentCommandResponse);
        candidate.setMetadata(payload.getStoredCandidateMetadata());
        candidate.setName(assertionName);
        candidate.setDescription(assertionDescription);
        candidate.setTestAssertions(atomicAssertion);

        listener.onSaved(candidate);
    }

    private String prepareString(String source) {
        if (source.equals("Optional")) {
            return "";
        } else {
            return source.trim();
        }
    }

    // private void setInfo() {
    //     boolean updated = false;
    //     String name = storedCandidate.getName();
    //     String description = storedCandidate.getDescription();

    //     if (name != null) {
    //         updated = true;
    //     }
    //     if (description != null) {
    //         updated = true;
    //     }
    //     if (updated) {
    //         this.saveButton.setText("Save & Close");
    //     }
    // }

    private String formatLocation(String location) {
        if (location.length() <= 59) {
            return location;
        } else {
            String left = location.substring(0, 47);
            left = left.substring(0, left.lastIndexOf("/") + 1);
            left += ".../unlogged/";
            return left;
        }
    }

    public DefaultMutableTreeNode getTree() {
        return JsonTreeUtils.buildJsonTree(agentCommandResponse.getMethodReturnValue(),
                getSimpleName(agentCommandResponse.getResponseClassName()));
    }

    private String getSimpleName(String qualifiedName) {
        if (qualifiedName == null) {
            return "";
        }
        String[] parts = qualifiedName.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : qualifiedName;
    }

    public AssertionBlock getRuleEditor() {
        return this.ruleEditor;
    }

    public JTree getCandidateExplorerTree() {
        return candidateExplorerTree;
    }

    public StoredCandidate getStoredCandidate() {
        return storedCandidate;
    }
}
