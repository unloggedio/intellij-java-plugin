package com.insidious.plugin.ui.Components.AtomicRecord;

import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.callbacks.CandidateLifeListener;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.util.AtomicRecordUtils;
import com.insidious.plugin.util.UIUtils;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SaveForm extends JFrame {

    private CandidateLifeListener listener;
    private JLabel title;
    private JLabel nameLabel;
    private JLabel descriptionLabel;
    private Container c;
    private JTextField nameField;
    private JTextArea description;
    private JButton saveButton;
    private JButton cancelButton;
    private SaveForm form = this;
    private JLabel assertionLabel;
    private ButtonGroup radioGroup = new ButtonGroup();
    private String selectedType = "Assert Equals";
    private StoredCandidate storedCandidate;
    private JRadioButton b1;
    private JRadioButton b2;
    private AgentCommandResponse agentCommandResponse;
    private final int height = 728;
    private final int width = 1000;

    //AgentCommandResponse is necessary for update flow and Assertions as well
    public SaveForm(StoredCandidate storedCandidate,
                    AgentCommandResponse<String> agentCommandResponse,
                    CandidateLifeListener listener) {
        this.storedCandidate = storedCandidate;
        this.listener = listener;
        this.agentCommandResponse = agentCommandResponse;
        setTitle("Unlogged Inc.");
        setBounds(400, 100, width, height);
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
        treeParent.setBorder(new EmptyBorder(0,0,0,0));

        JPanel treeViewer = new JPanel();
        treeViewer.setLayout(new BoxLayout(treeViewer,BoxLayout.Y_AXIS));
        treeViewer.setSize(570,260);
        treeViewer.setLocation(25,50);
        treeViewer.setBorder(new LineBorder(Color.cyan));
        treeViewer.add(Box.createRigidArea(new Dimension(0,5)));
        treeViewer.add(new JLabel("Available recorded objects"));
        treeViewer.add(Box.createRigidArea(new Dimension(0,5)));
        treeViewer.add(treeParent);
        c.add(treeViewer);

        JPanel metadataPanel = new SaveFormMetadataPanel().getMainPanel();
        metadataPanel.setLocation(595,50);
        metadataPanel.setSize(380,260);
        metadataPanel.setBorder(new LineBorder(Color.cyan));
        c.add(metadataPanel);

        JScrollPane editorPanel = getEditorPanel();
        editorPanel.setBorder(new LineBorder(Color.red));
        c.add(editorPanel);

        JLabel infoLabel = new JLabel("Case will be saved at "+
                formatLocation(listener.getSaveLocation()));
        infoLabel.setFont(new Font("Verdana", Font.PLAIN, 12));
        infoLabel.setSize(700, 12);
        infoLabel.setLocation(25, height-75);
        c.add(infoLabel);

        saveButton = new JButton("Save and Close");
        saveButton.setSize(150, 30);
        saveButton.setLocation(width-170, height-85);
        saveButton.setIcon(UIUtils.SAVE_CANDIDATE_GREY);
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //triggerSave();
            }
        });
        c.add(saveButton);

        cancelButton = new JButton("Cancel");
        cancelButton.setSize(100, 30);
        cancelButton.setLocation(width-270, height-85);

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                form.dispose();
            }
        });
        c.add(cancelButton);
    }

    private void triggerSave() {
        String name_text = prepareString(nameField.getText());
        String description_text = prepareString(description.getText());
        ButtonModel model = radioGroup.getSelection();
        StoredCandidate.AssertionType type = StoredCandidate.AssertionType.EQUAL;
        if (model.getActionCommand().equals("Assert Not Equals")) {
            type = StoredCandidate.AssertionType.NOT_EQUAL;
        }
        //this call is necessary
        //Required if we cancel update/save
        //Required for upcoming assertion flows as well
        StoredCandidate candidate = AtomicRecordUtils.createCandidateFor(storedCandidate, agentCommandResponse);
        candidate.setName(name_text);
        candidate.setDescription(description_text);
        candidate.setAssertionType(type);
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
        StoredCandidate.AssertionType assertionType = storedCandidate.getAssertionType();

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

    private DefaultMutableTreeNode getMockTree(){
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        int loops = 10;
        int counter=1;
        for(int i=0;i<loops;i++) {
            root.add(new DefaultMutableTreeNode("Elem "+counter++));
            root.add(new DefaultMutableTreeNode("Elem "+counter++));

            DefaultMutableTreeNode obj1 = new DefaultMutableTreeNode("Elem "+counter++);
            obj1.add(new DefaultMutableTreeNode("Child 1"));
            obj1.add(new DefaultMutableTreeNode("Child 2"));
            root.add(obj1);

            root.add(new DefaultMutableTreeNode("Elem "+counter++));
            root.add(new DefaultMutableTreeNode("Elem "+counter++));
        }
        return root;
    }

    //wip
    private JScrollPane getEditorPanel()
    {
        JPanel assertionPanel = new JPanel();
        assertionPanel.setLayout(new BoxLayout(assertionPanel,BoxLayout.Y_AXIS));

        JPanel defaultPanel = new JPanel();
        defaultPanel.add(new JLabel("Test label"));
        assertionPanel.add(defaultPanel);

        JScrollPane scrollPane = new JBScrollPane(assertionPanel);
        scrollPane.setLocation(25,320);
        scrollPane.setSize(950,310);
        return scrollPane;
    }


}
