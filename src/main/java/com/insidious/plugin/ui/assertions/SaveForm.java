package com.insidious.plugin.ui.assertions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.assertions.AssertionType;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.callbacks.CandidateLifeListener;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.pojo.atomic.TestType;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.ObjectMapperInstance;
import com.insidious.plugin.util.UIUtils;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

public class SaveForm implements OnTestTypeChangeListener {

    private static final Logger logger = LoggerUtil.getInstance(SaveForm.class);
    private final static ObjectMapper objectMapper = ObjectMapperInstance.getInstance();
    private final CandidateLifeListener candidateLifeListener;
    private final List<StoredCandidate> storedCandidateList;
    private final AssertionBlock ruleEditor;
    //    private final SaveFormMetadataPanel metadataForm;
    private final JPanel mainPanel;
    private final JTabbedPane bottomTabPanel;
    //    private JPanel mockPanel;
//    private HashSet<String> enabledMockList;
    //    private JsonNode responseNode;
    private JButton saveButton;
    // private JButton cancelButton;
//    private JTree candidateExplorerTree;
    private InsidiousService insidiousService;
    //AgentCommandResponse is necessary for update flow and Assertions as well
    private HashMap<JCheckBox, ArrayList<JCheckBox>> buttonMap = new HashMap<JCheckBox, ArrayList<JCheckBox>>();
    //    private MockValueMap mockValueMap;

    public SaveForm(
            List<StoredCandidate> storedCandidate,
            CandidateLifeListener clf
    ) throws JsonProcessingException {
        this.storedCandidateList = storedCandidate;
        this.candidateLifeListener = clf;
//        this.enabledMockList = new HashSet<>(storedCandidateList.getMockIds());

        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
//        candidateExplorerTree = new Tree(getTree());

//        String methodReturnValue = storedCandidate.getReturnValue();

//        responseNode = getResponseNode(methodReturnValue, storedCandidate.getReturnValueClassname());


        // clone the assertions
        AtomicAssertion existingAssertion = new AtomicAssertion();

        if (existingAssertion.getSubAssertions() == null || existingAssertion.getSubAssertions().size() == 0) {
            List<AtomicAssertion> subAssertions = new ArrayList<>();
            subAssertions.add(
                    new AtomicAssertion(AssertionType.EQUAL, "/", "methodReturnValue"));
            existingAssertion = new AtomicAssertion(AssertionType.ALLOF, subAssertions);
        }
        ruleEditor = new AssertionBlock(existingAssertion, new AssertionBlockManagerListener("value"), true);

        JPanel ruleEditorPanel = ruleEditor.getMainPanel();
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
        cancelButton.addActionListener(e -> candidateLifeListener.onCancel());
        buttonPanel.add(cancelButton);

        // save button in panel
        this.saveButton = new JButton();
        this.saveButton.setIcon(UIUtils.SAVE_CANDIDATE_GREEN_SVG);
        this.saveButton.setText("Save & Close");
        this.saveButton.addActionListener(e -> triggerSave());
        buttonPanel.add(this.saveButton);

        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        topPanel.add(buttonPanel);

//        JPanel midPanel = new JPanel();
//        midPanel.setLayout(new GridLayout(1, 2));

        // define treePanelHeading
        JLabel treePanelHeading = new JLabel();
        treePanelHeading.setText("<html><b>Available recorded objects:</b></html>");
        treePanelHeading.setVerticalAlignment(SwingConstants.TOP);
        treePanelHeading.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        // define treePanel


        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        mainPanel.setPreferredSize(new Dimension(-1,
                (int) (screenSize.getHeight() - (0.3 * screenSize.getHeight()))));


        // assertion panel
        JPanel assertionPanel = new JPanel();
        GridLayout assertionPanelLayout = new GridLayout(1, 1);
        assertionPanel.setLayout(assertionPanelLayout);

        JScrollPane assertionScrollPanel = new JBScrollPane(ruleEditorPanel);
        assertionPanel.add(assertionScrollPanel);

        // define lowerPanel
        bottomTabPanel = new JBTabbedPane();
        bottomTabPanel.addTab("Assertion", assertionPanel);
//        bottomTabPanel.addTab("Mock Data", this.mockPanel);

        bottomTabPanel.addChangeListener(e -> {
            JSONObject panelChanged = new JSONObject();
            panelChanged.put("tabIndex", bottomTabPanel.getSelectedIndex());
            UsageInsightTracker.getInstance().RecordEvent("MOCK_LINKING_TAB_TYPE", panelChanged);
        });
        JPanel primaryContentPanel = new JPanel();
        BoxLayout boxLayout = new BoxLayout(primaryContentPanel, BoxLayout.Y_AXIS);
        primaryContentPanel.setLayout(boxLayout);

        bottomTabPanel.setPreferredSize(new Dimension(-1, 360));
        primaryContentPanel.add(bottomTabPanel);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(primaryContentPanel, BorderLayout.CENTER);
    }


    public JPanel getComponent() {
        return mainPanel;
    }

    private void triggerSave() {
        AtomicAssertion atomicAssertion = ruleEditor.getAssertion();

        int mockCount = 0;
        for (StoredCandidate storedCandidate : storedCandidateList) {
            MetadataViewPayload metadataViewPayload = new MetadataViewPayload(storedCandidate.getName(),
                    storedCandidate.getDescription(), TestType.UNIT, storedCandidate.getMetadata());

            String assertionName = prepareString(metadataViewPayload.getName());
            String assertionDescription = prepareString(metadataViewPayload.getDescription());

            // set mocks
            if (metadataViewPayload.getType() == TestType.UNIT) {
                // unit test
                HashSet<String> enabledMockUnDeleted = new HashSet<>();
                for (DeclaredMock localMock : insidiousService.getDeclaredMocksFor(storedCandidate.getMethod())) {
//                    if (this.enabledMockList.contains(localMock.getId())) {
                    enabledMockUnDeleted.add(localMock.getId());
//                    }
                }

//            enabledMockUnDeleted = MockIntersection.enabledStoredMock(insidiousService, enabledMockUnDeleted);
                storedCandidate.setMockIds(enabledMockUnDeleted);
            } else {
                // integration test
                storedCandidate.setMockIds(new HashSet<String>());
            }

//            enabledMockList.clear();
            StoredCandidate candidate = StoredCandidate.createCandidateFor(storedCandidate);
            candidate.setMetadata(metadataViewPayload.getStoredCandidateMetadata());
            candidate.setName(assertionName);
            candidate.setDescription(assertionDescription);
            candidate.setTestAssertions(atomicAssertion);

            mockCount += candidateLifeListener.onSaved(candidate);
        }

        InsidiousNotification.notifyMessage(
                "Saved " + storedCandidateList.size() + " replay test and " + mockCount + " mock definitions",
                NotificationType.INFORMATION);


    }

    private String prepareString(String source) {
        if (source.equals("Optional")) {
            return "";
        } else {
            return source.trim();
        }
    }

    public AssertionBlock getRuleEditor() {
        return this.ruleEditor;
    }

    public List<StoredCandidate> getStoredCandidateList() {
        return storedCandidateList;
    }

    @Override
    public void onTestTypeChange(TestType updatedTestType) {
        if (Objects.equals(updatedTestType, TestType.UNIT)) {
            bottomTabPanel.setEnabledAt(1, true);
        } else if (updatedTestType == TestType.INTEGRATION) {
            bottomTabPanel.setEnabledAt(1, false);
            bottomTabPanel.setSelectedIndex(0);
        }

//        enabledMockList.clear();
    }
}