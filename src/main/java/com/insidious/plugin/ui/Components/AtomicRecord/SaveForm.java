package com.insidious.plugin.ui.Components.AtomicRecord;

import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.assertions.AssertionType;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.callbacks.CandidateLifeListener;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.util.AtomicRecordUtils;
import com.insidious.plugin.util.UIUtils;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultMutableTreeNode;
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

        JTree candidateExplorerTree = new Tree(getMockTree());
        JScrollPane treeParent = new JBScrollPane(candidateExplorerTree);
        treeParent.setBorder(JBUI.Borders.empty());

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

        JPanel metadataPanel = new SaveFormMetadataPanel().getMainPanel();
        metadataPanel.setLocation(595, 50);
        metadataPanel.setSize(380, 260);
        metadataPanel.setBorder(new LineBorder(JBColor.CYAN));
        c.add(metadataPanel);

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
        mockOpenRules();
    }

    private void mockOpenRules()
    {
        List<AssertionBlockElement> elements = new ArrayList<>();
        elements.add(new AssertionBlockElement(AssertionBlockElement.AssertionBlockElementType.RULE,
                null,generateBlock(),null));
        elements.add(new AssertionBlockElement(AssertionBlockElement.AssertionBlockElementType.CONNECTOR,
                "OR",null,null));
        elements.add(new AssertionBlockElement(AssertionBlockElement.AssertionBlockElementType.RULE,
                null,generateBlock(),null));
        ruleEditor.openSavedRules(elements);
    }

    private AssertionBlockModel generateBlock()
    {
        List<RuleData> ruleData = new ArrayList<>();
        ruleData.add(new RuleData("Where","mock2","equals","e",null));
        ruleData.add(new RuleData("AND",null,"equals",null,null));
        ruleData.add(new RuleData("OR","mock3",null,null,null));
        AssertionBlockModel model = new AssertionBlockModel(0,ruleData);
        return model;
    }

    private void printRuleSet() {
        System.out.println("RULE SET : "+ruleEditor.getRuleSet());
    }

    private void triggerSave() {
        String name_text = prepareString(nameField.getText());
        String description_text = prepareString(description.getText());
        ButtonModel model = radioGroup.getSelection();
        AssertionType type = AssertionType.EQUAL;
        if (model.getActionCommand().equals("Assert Not Equals")) {
            type = AssertionType.NOT_EQUAL;
        }
        //this call is necessary
        //Required if we cancel update/save
        //Required for upcoming assertion flows as well
        StoredCandidate candidate = AtomicRecordUtils.createCandidateFor(storedCandidate, agentCommandResponse);
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
            this.nameField.setText(name);
            updated = true;
        }
        if (description != null) {
            this.description.setText(description);
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
