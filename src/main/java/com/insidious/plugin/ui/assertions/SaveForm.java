package com.insidious.plugin.ui.assertions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.assertions.*;
import com.insidious.plugin.callbacks.CandidateLifeListener;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.util.JsonTreeUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SaveForm {

    private static final Logger logger = LoggerUtil.getInstance(SaveForm.class);
    private final static ObjectMapper objectMapper = new ObjectMapper();
    private final CandidateLifeListener listener;
    private final StoredCandidate storedCandidate;
    private final AgentCommandResponse<String> agentCommandResponse;
    private final AssertionBlock ruleEditor;
    private final SaveFormMetadataPanel metadataForm;
    private final JPanel mainPanel;
    private JsonNode responseNode;
    private JButton saveButton;
    // private JButton cancelButton;
    private JLabel assertionLabel;
    private JRadioButton b1;
    private JRadioButton b2;
    private JTree candidateExplorerTree;

    //AgentCommandResponse is necessary for update flow and Assertions as well
    public SaveForm(
            StoredCandidate storedCandidate,
            AgentCommandResponse<String> agentCommandResponse,
            CandidateLifeListener listener
    ) {
        this.storedCandidate = storedCandidate;
        this.listener = listener;
        this.agentCommandResponse = agentCommandResponse;

        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        candidateExplorerTree = new Tree(getTree());

        String methodReturnValue = agentCommandResponse.getMethodReturnValue();


        try {
            responseNode = objectMapper.readTree(methodReturnValue);
        } catch (JsonProcessingException e) {
            // this shouldn't happen
            if ("java.lang.String".equals(agentCommandResponse.getResponseClassName())
                    && !methodReturnValue.startsWith("\"")
                    && !methodReturnValue.endsWith("\"")) {
                try {
                    responseNode = objectMapper.readTree("\"" + methodReturnValue + "\"");
                } catch (JsonProcessingException e1) {
                    // failed to read as a json node
                    throw new RuntimeException(e1);
                }
            }

        }


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
        int saveTestCaseHeadingHeight = 30;

        JPanel ruleEditor = this.ruleEditor.getMainPanel();

        // define topPanel
        JPanel topPanel = new JPanel();
        GridLayout topPanelLayout = new GridLayout(1,2);
        topPanel.setLayout(topPanelLayout);

        // define saveTestCaseHeading
        JLabel saveTestCaseHeading = new JLabel();
        saveTestCaseHeading.setIcon(UIUtils.TESTTUBE);
        saveTestCaseHeading.setText("<html><b>Save Test Case</b></html>");
        Border treeTitleLabelBorder = JBUI.Borders.empty(10);
        saveTestCaseHeading.setBorder(treeTitleLabelBorder);

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
        topPanel.setSize(new Dimension(lowerPanelWidth, saveTestCaseHeadingHeight));
        topPanel.setPreferredSize(new Dimension(lowerPanelWidth, saveTestCaseHeadingHeight));
        topPanel.setMaximumSize(new Dimension(lowerPanelWidth, saveTestCaseHeadingHeight));
        topPanel.add(buttonPanel);

        JPanel midPanel = new JPanel();
        GridLayout midPanelLayout = new GridLayout(1, 2);
        midPanel.setLayout(midPanelLayout);

        // define treePanelHeading
        JLabel treePanelHeading = new JLabel();
        treePanelHeading.setText("<html><b>Available recorded objects:</b></html>");
        treePanelHeading.setVerticalAlignment(SwingConstants.TOP);
        // define treePanel
        JPanel treePanel = new JPanel();
        treePanel.setLayout(new BorderLayout());
        treePanel.add(treePanelHeading, BorderLayout.NORTH);

        int CandidateExplorerTreeHeight = 300;

        mainPanel.setMaximumSize(new Dimension(lowerPanelWidth, 600));
        candidateExplorerTree.setSize(new Dimension(400, CandidateExplorerTreeHeight));
        JScrollPane treeParent = new JBScrollPane(candidateExplorerTree);
        treePanel.add(treeParent, BorderLayout.CENTER);
        midPanel.add(treePanel);

        // define the metadataPanel
        metadataForm = new SaveFormMetadataPanel(new MetadataViewPayload(storedCandidate.getName(),
                storedCandidate.getDescription(),
                storedCandidate.getMetadata()));

        JPanel metadataFormPanel = metadataForm.getMainPanel();
        midPanel.add(metadataFormPanel);

        // assertion panel
        JPanel assertionPanel = new JPanel();
        GridLayout assertionPanelLayout = new GridLayout(1,1);
        assertionPanel.setLayout(assertionPanelLayout);

        JScrollPane assertionScrollPanel = new JBScrollPane(ruleEditor);
        assertionScrollPanel.setMaximumSize(new Dimension(lowerPanelWidth, lowerPanelHeight));
        assertionScrollPanel.setPreferredSize(new Dimension(lowerPanelWidth, lowerPanelHeight));
        assertionPanel.add(assertionScrollPanel);

        // mock panel
        JPanel mockPanel = new JPanel();
        GridLayout mockPanelLayout = new GridLayout(1,1);
        mockPanel.setLayout(mockPanelLayout);

        // define mockDataPanelContent
        JTextArea local = new JTextArea(10,10); // todo form
        JPanel mockDataPanelContent = new JPanel();
        mockDataPanelContent.setLayout(new BoxLayout(mockDataPanelContent, BoxLayout.PAGE_AXIS));


        // define applyMockLabel
        JLabel applyMockLabel = new JLabel();
        applyMockLabel.setText("<html><b>Apply Mock</b></html>");
        JPanel applyMockPanel = new JPanel();
        applyMockPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        applyMockPanel.add(applyMockLabel, BorderLayout.NORTH);
        applyMockPanel.setSize(new Dimension(lowerPanelWidth, 30));
        applyMockPanel.setPreferredSize(new Dimension(lowerPanelWidth, 30));
        applyMockPanel.setMaximumSize(new Dimension(lowerPanelWidth, 30));
        mockDataPanelContent.add(applyMockPanel);

        // define mockMethodNamePanel
        JLabel mockMethodNameLabel = new JLabel();
        mockMethodNameLabel.setText("Call Expression");
        mockMethodNameLabel.setIcon(UIUtils.MOCK_DATA);
        JPanel mockMethodNamePanel = new JPanel();
        mockMethodNamePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        mockMethodNamePanel.add(mockMethodNameLabel, BorderLayout.NORTH);
        mockMethodNamePanel.setSize(new Dimension(lowerPanelWidth, 30));
        mockMethodNamePanel.setPreferredSize(new Dimension(lowerPanelWidth, 30));
        mockMethodNamePanel.setMaximumSize(new Dimension(lowerPanelWidth, 30));

        // define mockMethodDependencyPanel
        JPanel mockMethodDependencyPanel = new JPanel();
        GridLayout twoColumnLayout = new GridLayout(1,2);
        mockMethodDependencyPanel.setLayout(twoColumnLayout);

        // define mockMethodDependencyPanelLeft
        JPanel mockMethodDependencyPanelLeft = new JPanel();
        JPanel leftText = new JPanel();
        GridLayout twoRowLayout = new GridLayout(2,1);
        leftText.setLayout(twoRowLayout);
        
        JLabel leftTextFirst = new JLabel();
        leftTextFirst.setText("method name");
        leftText.add(leftTextFirst);

        JLabel leftTextSecond = new JLabel();
        leftTextSecond.setIcon(UIUtils.CLASS_FILE);
        leftTextSecond.setText("class name");
        leftText.add(leftTextSecond);

        mockMethodDependencyPanelLeft.setLayout(new FlowLayout(FlowLayout.LEFT));
        mockMethodDependencyPanelLeft.add(leftText);
        mockMethodDependencyPanel.add(mockMethodDependencyPanelLeft);

        // define mockMethodDependencyPanelRight
        JPanel mockMethodDependencyPanelRight = new JPanel();
        JRadioButton mockButton = new JRadioButton();
        mockMethodDependencyPanelRight.setLayout(new FlowLayout(FlowLayout.RIGHT));
        mockMethodDependencyPanelRight.add(mockButton);
        mockMethodDependencyPanel.add(mockMethodDependencyPanelRight);

        mockMethodDependencyPanel.setSize(new Dimension(lowerPanelWidth, 50));
        mockMethodDependencyPanel.setPreferredSize(new Dimension(lowerPanelWidth, 50));
        mockMethodDependencyPanel.setMaximumSize(new Dimension(lowerPanelWidth, 50));

        JPanel mockMethodPanel = new JPanel();
        mockMethodPanel.setLayout(new BoxLayout(mockMethodPanel, BoxLayout.PAGE_AXIS));
        mockMethodPanel.add(mockMethodNamePanel);
        mockMethodPanel.add(mockMethodDependencyPanel);
        mockDataPanelContent.add(mockMethodPanel);

        // debugging border
        // applyMockPanel.setBorder(BorderFactory.createLineBorder(JBColor.RED));
        // mockMethodNamePanel.setBorder(BorderFactory.createLineBorder(JBColor.BLUE));
        mockMethodDependencyPanelLeft.setBorder(BorderFactory.createLineBorder(JBColor.GREEN));
        mockMethodDependencyPanelRight.setBorder(BorderFactory.createLineBorder(JBColor.GREEN));
        mockMethodDependencyPanel.setBorder(BorderFactory.createLineBorder(JBColor.RED));
        // mockMethodPanel.setBorder(BorderFactory.createLineBorder(JBColor.GREEN));
        // mockDataPanelContent.setBorder(BorderFactory.createLineBorder(JBColor.ORANGE));

        JScrollPane mockScrollPanel = new JBScrollPane(mockDataPanelContent);
        mockScrollPanel.setMaximumSize(new Dimension(lowerPanelWidth, lowerPanelHeight));
        mockScrollPanel.setPreferredSize(new Dimension(lowerPanelWidth, lowerPanelHeight));
        mockPanel.add(mockScrollPanel);

        // define lowerPanel
        JTabbedPane lowerPanel = new JBTabbedPane();
        lowerPanel.addTab("Assertion", assertionPanel);
        lowerPanel.addTab("Mock Data", mockPanel);
        System.out.println("Value: " + this.metadataForm.comboBox1.getSelectedItem().toString());
        lowerPanel.setEnabledAt(1, "Unit Test" == this.metadataForm.comboBox1.getSelectedItem().toString());

        // add panel in mainPanel
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(midPanel, BorderLayout.CENTER);
        mainPanel.add(lowerPanel, BorderLayout.SOUTH);


//        scrollPane.setLocation(25, 320);
//        scrollPane.setSize(950, 310);
        // metadataForm.getCancelButton().addActionListener(e -> listener.onCancel());
        // metadataForm.getSaveButton().addActionListener(e -> triggerSave());

//        JPanel bottomPanel = new JPanel();
//        bottomPanel.setLayout(new BorderLayout());

//        String saveLocation = listener.getSaveLocation();
//        JLabel infoLabel = new JLabel("Case will be saved at " + formatLocation(saveLocation));
//        infoLabel.setToolTipText(saveLocation);
//        infoLabel.setFont(new Font("Verdana", Font.PLAIN, 12));
//        infoLabel.setSize(400, 12);
//        bottomPanel.add(infoLabel, BorderLayout.WEST);

//        JPanel bottomPanelRight = new JPanel();
//        bottomPanelRight.setAlignmentX(1);

//        cancelButton = new JButton("Cancel");
//        cancelButton.setSize(100, 30);
//
//
//        saveButton = new JButton("Save and Close");
//        saveButton.setSize(150, 30);
//        saveButton.setIcon(UIUtils.SAVE_CANDIDATE_PINK);

//        bottomPanelRight.add(cancelButton);
//        bottomPanelRight.add(saveButton);

//        bottomPanel.add(bottomPanelRight, BorderLayout.EAST);
//        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
       // setInfo();
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

        //this call is necessary
        //Required if we cancel update/save
        //Required for upcoming assertion flows as well
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
