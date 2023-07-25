package com.insidious.plugin.ui.Components.AtomicRecord;

import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.assertions.AssertionType;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.callbacks.CandidateLifeListener;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.util.AtomicRecordUtils;
import com.insidious.plugin.util.JsonTreeUtils;
import com.insidious.plugin.util.UIUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class SaveForm extends JFrame {

    private final int height = 728;
    private final int width = 1000;
    private final ButtonGroup radioGroup = new ButtonGroup();
    private CandidateLifeListener listener;
    private JLabel title;
    private JLabel nameLabel;
    private JLabel descriptionLabel;
    private Container c;
    private JTextField nameField;
    private JTextArea description;
    private JButton saveButton;
    private JButton cancelButton;
    private JLabel assertionLabel;
    private StoredCandidate storedCandidate;
    private JRadioButton b1;
    private JRadioButton b2;
    private AgentCommandResponse<String> agentCommandResponse;
    private AssertionRuleEditorImpl ruleEditor = new AssertionRuleEditorImpl();
    private SaveFormMetadataPanel metadataPanel;

    //AgentCommandResponse is necessary for update flow and Assertions as well
    public SaveForm(StoredCandidate storedCandidate,
                    AgentCommandResponse<String> agentCommandResponse,
                    CandidateLifeListener listener) {
        this.storedCandidate = storedCandidate;
        this.listener = listener;
        this.agentCommandResponse = agentCommandResponse;
        setTitle("Unlogged Inc.");
        setBounds(200, 50, width, height);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        c = getContentPane();
        c.setLayout(null);

        title = new JLabel("Save Atomic Case");
        title.setFont(new Font("", Font.PLAIN, 18));
        title.setSize(300, 30);
        title.setLocation(25, 10);
        c.add(title);

        JTree candidateExplorerTree = new Tree(getTree());
        JScrollPane treeParent = new JBScrollPane(candidateExplorerTree);
        treeParent.setBorder(JBUI.Borders.empty());

        candidateExplorerTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                        candidateExplorerTree.getLastSelectedPathComponent();
                if (node == null) return;
                TreeNode[] nodes = node.getPath();
                ruleEditor.setCurrentKey(JsonTreeUtils.getFlatMap(nodes));
            }
        });
        TreePath path = candidateExplorerTree.getPathForRow(0);
        ruleEditor.setCurrentKey(JsonTreeUtils.getFlatMap(path.getPath()));

        JPanel treeViewer = new JPanel();
        treeViewer.setLayout(new BoxLayout(treeViewer, BoxLayout.Y_AXIS));
        treeViewer.setSize(570, 260);
        treeViewer.setLocation(25, 50);
        treeViewer.setBorder(new LineBorder(JBColor.CYAN));
        treeViewer.add(Box.createRigidArea(new Dimension(0, 5)));
        treeViewer.add(new JLabel("Available recorded objects"));
        treeViewer.add(Box.createRigidArea(new Dimension(0, 5)));
        treeViewer.add(treeParent);
        c.add(treeViewer);

        metadataPanel = new SaveFormMetadataPanel(new MetadataViewPayload(storedCandidate.getName(),
                storedCandidate.getDescription(),
                storedCandidate.getMetadata()));
        metadataPanel.getMainPanel().setLocation(595, 50);
        metadataPanel.getMainPanel().setSize(380, 260);
        metadataPanel.getMainPanel().setBorder(new LineBorder(JBColor.CYAN));
        c.add(metadataPanel.getMainPanel());

        JScrollPane editorPanel = getEditorPanel();
        editorPanel.setBorder(new LineBorder(JBColor.RED));
        c.add(editorPanel);

        JLabel infoLabel = new JLabel("Case will be saved at " +
                formatLocation(listener.getSaveLocation()));
        infoLabel.setFont(new Font("Verdana", Font.PLAIN, 12));
        infoLabel.setSize(700, 12);
        infoLabel.setLocation(25, height - 75);
        c.add(infoLabel);

        saveButton = new JButton("Save and Close");
        saveButton.setSize(150, 30);
        saveButton.setLocation(width - 170, height - 85);
        saveButton.setIcon(UIUtils.SAVE_CANDIDATE_GREY);
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //triggerSave();
                printRuleSet();
            }
        });
        c.add(saveButton);

        cancelButton = new JButton("Cancel");
        cancelButton.setSize(100, 30);
        cancelButton.setLocation(width - 270, height - 85);

        cancelButton.addActionListener(e -> SaveForm.this.dispose());
        c.add(cancelButton);
        setInfo();
    }

    private void printRuleSet() {
        List<AtomicAssertion> assertions = ruleEditor.getAtomicAssertions();
        System.out.println("RULE SET : "+assertions.toString());
        System.out.println("METADATA SET : "+metadataPanel.getPayload());

        //wip : to save, also to update toString for AtomicAssertions
        triggerSave();
    }

    private void triggerSave() {
        MetadataViewPayload payload = metadataPanel.getPayload();
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

    public DefaultMutableTreeNode getTree()
    {
        return JsonTreeUtils.buildJsonTree(agentCommandResponse.getMethodReturnValue()
                ,getSimpleName(agentCommandResponse.getResponseClassName()));
    }

    private String getSimpleName(String qualifiedName)
    {
        String[] parts = qualifiedName.split("\\.");
        String simpleName = parts.length>0 ? parts[parts.length-1] : qualifiedName;
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

    //wip
    private JScrollPane getEditorPanel() {
        JPanel editor = ruleEditor.getMainPanel();

        JScrollPane scrollPane = new JBScrollPane(editor);
        scrollPane.setLocation(25, 320);
        scrollPane.setSize(950, 310);
        return scrollPane;
    }


}
