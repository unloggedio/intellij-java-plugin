package com.insidious.plugin.ui.Components.AtomicRecord;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.assertions.*;
import com.insidious.plugin.callbacks.CandidateLifeListener;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.util.AtomicRecordUtils;
import com.insidious.plugin.util.JsonTreeUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
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
    private final JsonNode responseNode;
    private JButton saveButton;
    private JButton cancelButton;
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

        try {
            responseNode = objectMapper.readValue(agentCommandResponse.getMethodReturnValue(), JsonNode.class);
        } catch (JsonProcessingException e) {
            // this shouldn't happen
            throw new RuntimeException(e);
        }

        AtomicAssertion existingAssertion = storedCandidate.getTestAssertions();
        if (existingAssertion == null || existingAssertion.getSubAssertions() == null ||
                existingAssertion.getSubAssertions().size() == 0) {
            List<AtomicAssertion> subAssertions = new ArrayList<>();
            subAssertions.add(
                    new AtomicAssertion(AssertionType.EQUAL, "/", agentCommandResponse.getMethodReturnValue()));
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
                Object valueFromJsonNode = JsonTreeUtils.getValueFromJsonNode(
                        methodReturnValue, selectedKey);
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

        JLabel treeTitleLabel = new JLabel("Available recorded objects");
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


        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());

        JPanel bottomPanelLeft = new JPanel();
        bottomPanelLeft.setAlignmentX(0);

        JLabel infoLabel = new JLabel("Case will be saved at " + formatLocation(listener.getSaveLocation()));
        infoLabel.setFont(new Font("Verdana", Font.PLAIN, 12));
        infoLabel.setSize(700, 12);
        bottomPanelLeft.add(infoLabel);

        JPanel bottomPanelRight = new JPanel();
        bottomPanelRight.setAlignmentX(1);

        cancelButton = new JButton("Cancel");
        cancelButton.setSize(100, 30);

        cancelButton.addActionListener(e -> listener.onCancel());

        saveButton = new JButton("Save and Close");
        saveButton.setSize(150, 30);
        saveButton.setIcon(UIUtils.SAVE_CANDIDATE_GREY);
        saveButton.addActionListener(e -> triggerSave());

        bottomPanelRight.add(cancelButton);
        bottomPanelRight.add(saveButton);

        bottomPanel.add(bottomPanelLeft, BorderLayout.WEST);
        bottomPanel.add(bottomPanelRight, BorderLayout.EAST);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        setInfo();
    }

    public JPanel getComponent() {
        return mainPanel;
    }

    private void triggerSave() {

        AtomicAssertion atomicAssertion = ruleEditor.getAssertion();
//        try {
//            logger.warn("Atomic assertion: \n" +
//                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(atomicAssertion));
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }

        MetadataViewPayload payload = metadataForm.getPayload();
        String name_text = prepareString(payload.getName());
        String description_text = prepareString(payload.getDescription());
        AssertionType type = AssertionType.EQUAL;
//        if (model.getActionCommand().equals("Assert Not Equals")) {
//            type = AssertionType.NOT_EQUAL;
//        }
        //this call is necessary
        //Required if we cancel update/save
        //Required for upcoming assertion flows as well
        StoredCandidate candidate = AtomicRecordUtils.createCandidateFor(storedCandidate, agentCommandResponse);
        candidate.setMetadata(payload.getStoredCandidateMetadata());
        candidate.setName(name_text);
        candidate.setDescription(description_text);
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
            saveButton.setText("Update");
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
        String[] parts = qualifiedName.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : qualifiedName;
    }

    public AssertionBlock getRuleEditor()
    {
        return this.ruleEditor;
    }

    public JTree getCandidateExplorerTree() {
        return candidateExplorerTree;
    }

    public StoredCandidate getStoredCandidate() {
        return storedCandidate;
    }
}
