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
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.components.JBScrollPane;
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
    //    private JButton saveButton;
//    private JButton cancelButton;
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
        int topPanelHeight = 200;

        JPanel ruleEditor = this.ruleEditor.getMainPanel();

        JScrollPane assertionScrollPanel = new JBScrollPane(ruleEditor);
//        scrollPane.setLocation(25, 320);
//        scrollPane.setSize(950, 310);
        assertionScrollPanel.setMaximumSize(new Dimension(1080, 300));
        assertionScrollPanel.setPreferredSize(new Dimension(1080, 300));


        JPanel topPanel = new JPanel();

        GridLayout topPanelLayout = new GridLayout(1, 2);
        topPanel.setLayout(topPanelLayout);

        mainPanel.setMaximumSize(new Dimension(1080, 600));

        candidateExplorerTree.setSize(new Dimension(400, 300));

        JScrollPane treeParent = new JBScrollPane(candidateExplorerTree);
        treeParent.setSize(new Dimension(400, topPanelHeight));
        treeParent.setMaximumSize(new Dimension(400, topPanelHeight));
        treeParent.setPreferredSize(new Dimension(400, topPanelHeight));


        JPanel treeViewer = new JPanel();
        treeViewer.setLayout(new BorderLayout());

        JLabel treeTitleLabel = new JLabel();
        treeTitleLabel.setText("<html><b>Available recorded objects</b></html>");
        JPanel titleLabelContainer = new JPanel();

        Border border = treeTitleLabel.getBorder();
        Border margin = JBUI.Borders.empty(10);
        CompoundBorder borderWithMargin = new CompoundBorder(border, margin);
        treeTitleLabel.setBorder(borderWithMargin);

        titleLabelContainer.add(treeTitleLabel);
        treeViewer.add(treeTitleLabel, BorderLayout.NORTH);
//        treeViewer.add(Box.createRigidArea(new Dimension(0, 5)));
        treeViewer.add(treeParent, BorderLayout.CENTER);

        //        objectScroller.setMaximumSize(new Dimension(300, 400));
        treeViewer.setSize(new Dimension(400, topPanelHeight));
        treeViewer.setMaximumSize(new Dimension(400, topPanelHeight));
        treeViewer.setPreferredSize(new Dimension(400, topPanelHeight));
        topPanel.add(treeViewer);


        metadataForm = new SaveFormMetadataPanel(new MetadataViewPayload(storedCandidate.getName(),
                storedCandidate.getDescription(),
                storedCandidate.getMetadata()));

        JPanel metadataFormPanel = metadataForm.getMainPanel();
        metadataFormPanel.setSize(new Dimension(380, topPanelHeight));
        metadataFormPanel.setMaximumSize(new Dimension(380, topPanelHeight));

        topPanel.add(metadataFormPanel);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(assertionScrollPanel, BorderLayout.CENTER);

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
//
        metadataForm.getCancelButton().addActionListener(e -> listener.onCancel());
        metadataForm.getSaveButton().addActionListener(e -> triggerSave());

//        bottomPanelRight.add(cancelButton);
//        bottomPanelRight.add(saveButton);

//        bottomPanel.add(bottomPanelRight, BorderLayout.EAST);
//        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        setInfo();
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

    private void setInfo() {
        boolean updated = false;
        String name = storedCandidate.getName();
        String description = storedCandidate.getDescription();

        if (name != null) {
            updated = true;
        }
        if (description != null) {
            updated = true;
        }
        if (updated) {
            metadataForm.getSaveButton().setText("Update");
        }
    }

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
