package com.insidious.plugin.ui.assertions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.assertions.*;
import com.insidious.plugin.callbacks.CandidateLifeListener;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.pojo.atomic.TestType;
import com.insidious.plugin.ui.mocking.MockDefinitionEditor;
import com.insidious.plugin.ui.mocking.OnSaveListener;
import com.insidious.plugin.util.*;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.*;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;

import static com.insidious.plugin.util.ParameterUtils.processResponseForFloatAndDoubleTypes;

public class SaveForm implements OnTestTypeChangeListener, OnSaveListener {

    private static final Logger logger = LoggerUtil.getInstance(SaveForm.class);
    private final static ObjectMapper objectMapper = ObjectMapperInstance.getInstance();
    private final CandidateLifeListener listener;
    private final StoredCandidate storedCandidate;
    private final AgentCommandResponse<String> agentCommandResponse;
    private final AssertionBlock ruleEditor;
    private final SaveFormMetadataPanel metadataForm;
    private final JPanel mainPanel;
    private final JTabbedPane bottomTabPanel;
    private JPanel mockPanel;
    private HashSet<String> enabledMockList;
    private JsonNode responseNode;
    private JButton saveButton;
    // private JButton cancelButton;
    private JTree candidateExplorerTree;
    private InsidiousService insidiousService;
    //AgentCommandResponse is necessary for update flow and Assertions as well
    private HashMap<JCheckBox, ArrayList<JCheckBox>> buttonMap = new HashMap<JCheckBox, ArrayList<JCheckBox>>();
    private MockValueMap mockValueMap;
    private SaveForm self = this;

    public SaveForm(
            StoredCandidate storedCandidate,
            AgentCommandResponse<String> agentCommandResponse,
            CandidateLifeListener listener
    ) {
        this.storedCandidate = storedCandidate;
        this.listener = listener;
        agentCommandResponse.setMethodReturnValue(
                processResponseForFloatAndDoubleTypes(agentCommandResponse.getResponseClassName(),
                        agentCommandResponse.getMethodReturnValue()));
        this.agentCommandResponse = agentCommandResponse;
        this.enabledMockList = new HashSet<>(this.storedCandidate.getMockIds());

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

        // define the metadataPanel
        metadataForm = new SaveFormMetadataPanel(new MetadataViewPayload(storedCandidate.getName(),
                storedCandidate.getDescription(), TestType.UNIT, storedCandidate.getMetadata()), this);

        JPanel metadataFormPanel = metadataForm.getMainPanel();

        midPanel.add(metadataFormPanel);
        midPanel.add(treePanel);

        // assertion panel
        JPanel assertionPanel = new JPanel();
        GridLayout assertionPanelLayout = new GridLayout(1, 1);
        assertionPanel.setLayout(assertionPanelLayout);

        JScrollPane assertionScrollPanel = new JBScrollPane(ruleEditor);
        assertionPanel.add(assertionScrollPanel);

        // mock panel
        this.mockPanel = new JPanel();
        addDataMockPanel();

        // define lowerPanel
        bottomTabPanel = new JBTabbedPane();
        bottomTabPanel.addTab("Assertion", assertionPanel);
        bottomTabPanel.addTab("Mock Data", this.mockPanel);

        bottomTabPanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSONObject panelChanged = new JSONObject();
                panelChanged.put("tabIndex", bottomTabPanel.getSelectedIndex());
                UsageInsightTracker.getInstance().RecordEvent("MOCK_LINKING_TAB_TYPE", panelChanged);
            }
        });
        JPanel primaryContentPanel = new JPanel();
        BoxLayout boxLayout = new BoxLayout(primaryContentPanel, BoxLayout.Y_AXIS);
        primaryContentPanel.setLayout(boxLayout);

        midPanel.setPreferredSize(new Dimension(-1, 320));
        bottomTabPanel.setPreferredSize(new Dimension(-1, 360));

        primaryContentPanel.add(midPanel);
        primaryContentPanel.add(bottomTabPanel);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(primaryContentPanel, BorderLayout.CENTER);
    }

    private void addDataMockPanel() {

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

        this.insidiousService = this.listener.getProject().getService(InsidiousService.class);
        this.mockValueMap = new MockValueMap(insidiousService);

        // define mockMethodPanel
        JPanel mockMethodPanel = new JPanel();
        mockMethodPanel.setLayout(new BoxLayout(mockMethodPanel, BoxLayout.Y_AXIS));

        for (String localKey : this.mockValueMap.getDependencyMockMap().keySet()) {
            mockMethodPanel.add(this.genMockMethodPanelSingle(localKey));
            mockMethodPanel.add(Box.createRigidArea(new Dimension(1, 10)));
        }

        mockDataPanelContent.add(mockMethodPanel);
        JScrollPane mockScrollPanel = new JBScrollPane(mockDataPanelContent);
        mockScrollPanel.setBorder(BorderFactory.createEmptyBorder());
        mockPanel.add(mockScrollPanel);
    }

    public JPanel genMockMethodPanelSingle(String localKey) {
        ArrayList<String> localKeyData = this.mockValueMap.getDependencyMockMap().get(localKey);
        PsiMethodCallExpression psiMethodCallExpression = this.mockValueMap.getReferenceToPsiMethodCallExpression().get(localKey);
        JPanel mockMethodPanelSingle = new JPanel();
        mockMethodPanelSingle.setLayout(new BoxLayout(mockMethodPanelSingle, BoxLayout.Y_AXIS));

        // define mockMethodNamePanel
        JPanel mockMethodNamePanel = new JPanel();
        GridLayout namePanelLayout = new GridLayout(1, 2);
        mockMethodNamePanel.setLayout(namePanelLayout);

        // define mockMethodNamePanelLeft
        JLabel mockMethodNamePanelLeft = new JLabel();
        mockMethodNamePanelLeft.setText(localKey);
        mockMethodNamePanelLeft.setIcon(UIUtils.MOCK_DATA);
        mockMethodNamePanelLeft.setBorder(JBUI.Borders.empty(4, 8, 4, 0));
        mockMethodNamePanel.add(mockMethodNamePanelLeft);

        // define mockMethodNamePanelRight
        JPanel mockMethodNamePanelRight = new JPanel();
        mockMethodNamePanelRight.setLayout(new FlowLayout(FlowLayout.RIGHT));

        JCheckBox mockButtonMain = new JCheckBox();
        this.buttonMap.put(mockButtonMain, new ArrayList<JCheckBox>());
        mockButtonMain.setSelected(!localKeyData.isEmpty() && this.storedCandidate.getMockIds().containsAll(localKeyData));
        ArrayList<JCheckBox> mockButtonMainPart = this.buttonMap.get(mockButtonMain);

        mockButtonMain.addActionListener(e -> {
            if (mockButtonMain.isSelected()) {
                this.stateInvertAllMocks(this.mockValueMap.getDependencyMockMap().get(localKey), true);
                for (int i = 0; i <= mockButtonMainPart.size() - 1; i++) {
                    mockButtonMainPart.get(i).setSelected(true);
                }
            } else {
                this.stateInvertAllMocks(this.mockValueMap.getDependencyMockMap().get(localKey), false);
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
        mockMethodPanelSingle.add(mockMethodNamePanel);

        for (int i = 0; i <= localKeyData.size() - 1; i++) {
            mockMethodPanelSingle.add(this.getMockMethodDependencyPanel(mockButtonMain, localKeyData.get(i)));
        }

        // configure new mock panel
        JPanel addMockPanel = new JPanel();
        addMockPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JButton addMockButton = getAddMockButton(psiMethodCallExpression);
        addMockPanel.add(addMockButton);

        addMockPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.BLACK));
        addMockPanel.setMaximumSize(new Dimension(3999, 50));
        mockMethodPanelSingle.add(addMockPanel);

        mockMethodPanelSingle.setBorder(BorderFactory.createLineBorder(JBColor.BLACK));
        mockMethodPanelSingle.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, JBColor.BLACK));

        return mockMethodPanelSingle;
    }

    private JButton getAddMockButton(PsiMethodCallExpression psiMethodCallExpression) {
        JButton addMockButton = new JButton();
        addMockButton.setText("Add Mock");
        addMockButton.setIcon(UIUtils.MOCK_ADD);
        addMockButton.setPreferredSize(new Dimension(100, 25));
        addMockButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Project project = insidiousService.getProject();
                JBPopup editorPopup = null;

                PsiMethod targetMethod = psiMethodCallExpression.resolveMethod();
                MethodUnderTest methodUnderTest = MethodUnderTest.fromMethodAdapter(new JavaMethodAdapter(targetMethod));
                MockDefinitionEditor mockDefinitionEditor = new MockDefinitionEditor(methodUnderTest, psiMethodCallExpression, project, self);
                JComponent gutterMethodComponent = mockDefinitionEditor.getComponent();
                ComponentPopupBuilder gutterMethodComponentPopup = JBPopupFactory.getInstance()
                        .createComponentPopupBuilder(gutterMethodComponent, null);
                editorPopup = gutterMethodComponentPopup
                        .setProject(psiMethodCallExpression.getProject())
                        .setShowBorder(true)
                        .setShowShadow(true)
                        .setFocusable(true)
                        .setRequestFocus(true)
                        .setResizable(true)
                        .setCancelOnClickOutside(true)
                        .setCancelOnOtherWindowOpen(true)
                        .setCancelKeyEnabled(true)
                        .setBelongsToGlobalPopupStack(false)
                        .setTitle("Mock Editor")
                        .addListener(new JBPopupListener() {
                            @Override
                            public void onClosed(LightweightWindowEvent event) {
                                JBPopupListener.super.onClosed(event);
                            }
                        })
                        .setTitleIcon(new ActiveIcon(UIUtils.ICON_EXECUTE_METHOD_SMALLER))
                        .createPopup();
                editorPopup.showUnderneathOf(addMockButton);

                mockDefinitionEditor.setPopupHandle(editorPopup);
            }
        });
        return addMockButton;
    }

    public JPanel getMockMethodDependencyPanel(JCheckBox mockButtonMain, String mockDataId) {

        // define mockMethodDependencyPanel
        JPanel mockMethodDependencyPanel = new JPanel();
        GridLayout dependencyGrid = new GridLayout(1, 2);
        dependencyGrid.setHgap(8);
        dependencyGrid.setVgap(4);
        mockMethodDependencyPanel.setLayout(dependencyGrid);

        // define mockMethodDependencyPanelLeft
        JPanel mockMethodDependencyPanelLeft = new JPanel();

        JLabel leftText = new JLabel();
        leftText.setText(this.mockValueMap.getMockNameIdMap().get(mockDataId));

        mockMethodDependencyPanelLeft.setLayout(new FlowLayout(FlowLayout.LEFT));
        mockMethodDependencyPanelLeft.add(leftText);
        mockMethodDependencyPanel.add(mockMethodDependencyPanelLeft);

        // define mockMethodDependencyPanelRight
        JPanel mockMethodDependencyPanelRight = new JPanel();
        mockMethodDependencyPanelRight.setLayout(new FlowLayout(FlowLayout.RIGHT));
        JCheckBox mockButton = new JCheckBox();
        mockButton.setSelected(this.storedCandidate.getMockIds().contains(mockDataId));
        mockButton.addActionListener(e -> {
            if (mockButton.isSelected()) {
                this.stateInvertSingleMock(mockDataId, true);
            } else {
                this.stateInvertSingleMock(mockDataId, false);
            }
        });
        mockMethodDependencyPanelRight.add(mockButton);
        this.buttonMap.get(mockButtonMain).add(mockButton);
        mockMethodDependencyPanel.add(mockMethodDependencyPanelRight);

        mockMethodDependencyPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.BLACK));
        mockMethodDependencyPanel.setMaximumSize(new Dimension(3999, 50));

        return mockMethodDependencyPanel;
    }

    private void stateInvertAllMocks(List<String> allDeclaredMocks, boolean state) {
        for (int i = 0; i <= allDeclaredMocks.size() - 1; i++) {
            if (state) {
                this.enabledMockList.add(allDeclaredMocks.get(i));
            } else {
                this.enabledMockList.remove(allDeclaredMocks.get(i));
            }
        }
    }

    private void stateInvertSingleMock(String localMock, boolean state) {
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

        // set mocks
        if (payload.getType() == TestType.UNIT) {
            // unit test
            HashSet<String> enabledMockUnDeleted = new HashSet<String>();
            for (DeclaredMock localMock : this.insidiousService.getDeclaredMocksFor(this.storedCandidate.getMethod())) {
                if (this.enabledMockList.contains(localMock.getId())) {
                    enabledMockUnDeleted.add(localMock.getId());
                }
            }

            enabledMockUnDeleted = MockIntersection.enabledStoredMock(insidiousService, enabledMockUnDeleted);
            this.storedCandidate.setMockIds(enabledMockUnDeleted);
        } else {
            // integration test
            this.storedCandidate.setMockIds(new HashSet<String>());
        }

        this.enabledMockList.clear();
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

    @Override
    public void onTestTypeChange(TestType updatedTestType) {
        if (Objects.equals(updatedTestType, TestType.UNIT)) {
            bottomTabPanel.setEnabledAt(1, true);
        } else if (updatedTestType == TestType.INTEGRATION) {
            bottomTabPanel.setEnabledAt(1, false);
            bottomTabPanel.setSelectedIndex(0);
        }

        enabledMockList.clear();
    }

    @Override
    public void onSaveDeclaredMock(DeclaredMock declaredMock, MethodUnderTest methodUnderTest) {
        insidiousService.saveMockDefinition(declaredMock, methodUnderTest);

        this.mockPanel.removeAll();
        addDataMockPanel();
    }
}