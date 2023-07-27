package com.insidious.plugin.ui.Components.AtomicRecord;

import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.assertions.AssertionType;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.callbacks.CandidateLifeListener;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.util.AtomicRecordUtils;
import com.insidious.plugin.util.JsonTreeUtils;
import com.insidious.plugin.util.UIUtils;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class SaveForm extends JFrame {

    private final int height = 728;
    private final int width = 1000;
    private final ButtonGroup radioGroup = new ButtonGroup();
    private final CandidateLifeListener listener;
    private final StoredCandidate storedCandidate;
    private final AgentCommandResponse<String> agentCommandResponse;
    private final AssertionBlock ruleEditor;
    private final SaveFormMetadataPanel metadataForm;
    private final JPanel mainPanel;
    //    private final List<Component> components;
    private JLabel title;
    private JLabel nameLabel;
    private JLabel descriptionLabel;
    //    private Container contentPane;
    private JTextField nameField;
    private JTextArea description;
    private JButton saveButton;
    private JButton cancelButton;
    private JLabel assertionLabel;
    private JRadioButton b1;
    private JRadioButton b2;

    //AgentCommandResponse is necessary for update flow and Assertions as well
    public SaveForm(StoredCandidate storedCandidate,
                    AgentCommandResponse<String> agentCommandResponse,
                    CandidateLifeListener listener) {
        this.storedCandidate = storedCandidate;
        this.listener = listener;
        this.agentCommandResponse = agentCommandResponse;

        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        JPanel titlePanel = new JPanel();

//        setTitle("Unlogged Inc.");
//        setBounds(200, 50, width, height);
//        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
//        setResizable(false);
        JTree candidateExplorerTree = new Tree(getTree());

        ruleEditor = new AssertionBlock(new AssertionBlockManager() {
            @Override
            public void addNewRule() {

            }

            @Override
            public void addNewGroup() {

            }

            @Override
            public void removeAssertionElement(AssertionElement element) {

            }

            @Override
            public void removeAssertionGroup() {

            }

            @Override
            public String getCurrentTreeKey() {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                        candidateExplorerTree.getLastSelectedPathComponent();
                if (node == null) return "root";
                TreeNode[] nodes = node.getPath();
                return JsonTreeUtils.getFlatMap(nodes);
            }

            @Override
            public void removeAssertionBlock(AssertionBlock block) {

            }
        });
        int topPanelHeight = 200;

        JPanel ruleEditor = this.ruleEditor.getMainPanel();

        JScrollPane assertionScrollPanel = new JBScrollPane(ruleEditor);
//        scrollPane.setLocation(25, 320);
//        scrollPane.setSize(950, 310);
        assertionScrollPanel.setMaximumSize(new Dimension(1080, 300));
        assertionScrollPanel.setPreferredSize(new Dimension(1080, 300));
//        JPanel midPanel = new JPanel(new BorderLayout());
//        midPanel.add(assertionScrollPanel, BorderLayout.CENTER);
//        midPanel.setMaximumSize(new Dimension(1080, 400));
//        midPanel.setMaximumSize(new Dimension(-1, 400));
//        new Thread(() -> {
//            while (true) {
//                try {
//                    Thread.sleep(3000);
//                    int currentWidth = assertionScrollPanel.getWidth();
//                    assertionScrollPanel.setSize(new Dimension(currentWidth, 500));
//                    assertionScrollPanel.revalidate();
//                    assertionScrollPanel.repaint();
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        }).start();


//        contentPane = getContentPane();
//        contentPane.setLayout(null);
//        components = new ArrayList<>();

        JPanel topPanel = new JPanel();
//        topPanel.setSize(new Dimension(800, 400));
//        topPanel.setMaximumSize(new Dimension(800, 400));
//        topPanel.setPreferredSize(new Dimension(800, 400));
        GridLayout mgr = new GridLayout(1, 2);
        topPanel.setLayout(mgr);
        title = new JLabel("Save Atomic Case");
        title.setFont(new Font("", Font.PLAIN, 18));
        title.setSize(300, 30);
//        title.setLocation(25, 10);
//        contentPane.add(title);
        titlePanel.add(title);
//        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.setMaximumSize(new Dimension(1080, 600));

        candidateExplorerTree.setSize(new Dimension(400, 300));
//        candidateExplorerTree.setBorder(new LineBorder(JBColor.GREEN));
        JScrollPane treeParent = new JBScrollPane(candidateExplorerTree);
        treeParent.setSize(new Dimension(400, topPanelHeight));
        treeParent.setMaximumSize(new Dimension(400, topPanelHeight));
        treeParent.setPreferredSize(new Dimension(400, topPanelHeight));
//        treeParent.setBorder(new LineBorder(JBColor.RED));

//        candidateExplorerTree.addTreeSelectionListener(new TreeSelectionListener() {
//            public void valueChanged(TreeSelectionEvent e) {
//                DefaultMutableTreeNode node = (DefaultMutableTreeNode)
//                        candidateExplorerTree.getLastSelectedPathComponent();
//                if (node == null) return;
//                TreeNode[] nodes = node.getPath();
//                ruleEditor.setCurrentKey(JsonTreeUtils.getFlatMap(nodes));
//            }
//        });


        JPanel treeViewer = new JPanel();
        treeViewer.setLayout(new BorderLayout());
//        treeViewer.setSize(-1, -1);
//        treeViewer.setLocation(25, 50);
//        treeViewer.setBorder(new LineBorder(JBColor.CYAN));
//        treeViewer.add(Box.createRigidArea(new Dimension(0, 5)));
        JLabel treeTitleLabel = new JLabel("Available recorded objects");
        JPanel titleLabelContainer = new JPanel();

        Border border = treeTitleLabel.getBorder();
        Border margin = JBUI.Borders.empty(10);
        treeTitleLabel.setBorder(new CompoundBorder(border, margin));

        titleLabelContainer.add(treeTitleLabel);
        treeViewer.add(treeTitleLabel, BorderLayout.NORTH);
//        treeViewer.add(Box.createRigidArea(new Dimension(0, 5)));
        treeViewer.add(treeParent, BorderLayout.CENTER);

        //        objectScroller.setMaximumSize(new Dimension(300, 400));
        treeViewer.setSize(new Dimension(400, topPanelHeight));
        treeViewer.setMaximumSize(new Dimension(400, topPanelHeight));
        treeViewer.setPreferredSize(new Dimension(400, topPanelHeight));
        topPanel.add(treeViewer);
//        topPanel.setMaximumSize(new Dimension(100, 300));
//        topPanel.setSize(new Dimension(-1, 300));
//        components.add(treeViewer);

        metadataForm = new SaveFormMetadataPanel(new MetadataViewPayload(storedCandidate.getName(),
                storedCandidate.getDescription(),
                storedCandidate.getMetadata()));
//        metadataPanel.getMainPanel().setLocation(595, 50);
        JPanel metadataFormPanel = metadataForm.getMainPanel();
        metadataFormPanel.setSize(new Dimension(380, topPanelHeight));
        metadataFormPanel.setMaximumSize(new Dimension(380, topPanelHeight));
//        metadataFormPanel.setBorder(new LineBorder(JBColor.CYAN));
//        components.add(metadataPanel.getMainPanel());
        topPanel.add(metadataFormPanel);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(assertionScrollPanel, BorderLayout.CENTER);

//        JScrollPane editorPanel = getEditorPanel();
//        editorPanel.setBorder(new LineBorder(JBColor.RED));

//        components.add(editorPanel);

        JPanel bottomPanel = new JPanel();
        JLabel infoLabel = new JLabel("Case will be saved at " +
                formatLocation(listener.getSaveLocation()));
        infoLabel.setFont(new Font("Verdana", Font.PLAIN, 12));
        infoLabel.setSize(700, 12);
//        infoLabel.setLocation(25, height - 75);
        bottomPanel.add(infoLabel);

        saveButton = new JButton("Save and Close");
        saveButton.setSize(150, 30);
//        saveButton.setLocation(width - 170, height - 85);
        saveButton.setIcon(UIUtils.SAVE_CANDIDATE_GREY);
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //triggerSave();
                printRuleSet();
            }
        });
        bottomPanel.add(saveButton);

        cancelButton = new JButton("Cancel");
        cancelButton.setSize(100, 30);
//        cancelButton.setLocation(width - 270, height - 85);

        cancelButton.addActionListener(e -> SaveForm.this.dispose());
        bottomPanel.add(cancelButton);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        setInfo();
    }

    public JPanel getComponent() {
        return mainPanel;
    }

    private void printRuleSet() {
        List<AtomicAssertion> assertions = ruleEditor.getAtomicAssertions();
        System.out.println("RULE SET : " + assertions.toString());
        System.out.println("METADATA SET : " + metadataForm.getPayload());

        //wip : to save, also to update toString for AtomicAssertions
        triggerSave();
    }

    private void triggerSave() {
        MetadataViewPayload payload = metadataForm.getPayload();
        String name_text = prepareString(payload.getName());
        String description_text = prepareString(payload.getDescription());
        ButtonModel model = radioGroup.getSelection();
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
        candidate.addTestAssertion(new AtomicAssertion(type, "root", ""));
        this.listener.onSaved(candidate);
        this.dispose();
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
        return JsonTreeUtils.buildJsonTree(agentCommandResponse.getMethodReturnValue()
                , getSimpleName(agentCommandResponse.getResponseClassName()));
    }

    private String getSimpleName(String qualifiedName) {
        String[] parts = qualifiedName.split("\\.");
        String simpleName = parts.length > 0 ? parts[parts.length - 1] : qualifiedName;
        return simpleName.toLowerCase();
    }

    private DefaultMutableTreeNode getMockTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        int loops = 10;
        int counter = 1;
        for (int i = 0; i < loops; i++) {
            root.add(new DefaultMutableTreeNode("Elem " + counter++));
            root.add(new DefaultMutableTreeNode("Elem " + counter++));

            DefaultMutableTreeNode obj1 = new DefaultMutableTreeNode("Elem " + counter++);
            obj1.add(new DefaultMutableTreeNode("Child 1"));
            obj1.add(new DefaultMutableTreeNode("Child 2"));
            root.add(obj1);

            root.add(new DefaultMutableTreeNode("Elem " + counter++));
            root.add(new DefaultMutableTreeNode("Elem " + counter++));
        }
        return root;
    }


}
