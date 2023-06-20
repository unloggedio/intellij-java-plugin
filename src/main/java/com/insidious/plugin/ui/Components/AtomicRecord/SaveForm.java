package com.insidious.plugin.ui.Components.AtomicRecord;

import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.util.UIUtils;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class SaveForm extends JFrame {

    private AtomicRecordListener listener;
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

    public SaveForm(AtomicRecordListener listener)
    {
        this.listener = listener;
        setTitle("Unlogged Inc.");
        setBounds(400, 160, 500, 500);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        c = getContentPane();
        c.setLayout(null);

        title = new JLabel("Save Atomic Case");
        title.setFont(new Font("", Font.PLAIN, 20));
        title.setSize(300, 30);
        title.setLocation(50, 15);
        c.add(title);

        nameLabel = new JLabel("Test Name:");
        nameLabel.setSize(100, 30);
        nameLabel.setLocation(50, 50);
        c.add(nameLabel);

        nameField = new JTextField();
        nameField.setSize(400, 30);
        nameField.setLocation(50, 50+24);
        nameField.setToolTipText("Enter name for the case");
        nameField.setText("Optional");
        nameField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                nameField.setForeground(UIManager.getColor("TextArea.foreground"));
                if(nameField.getText().equals("Optional")) {
                    nameField.setText("");
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (nameField.getText().length() == 0) {
                    nameField.setForeground(Color.GRAY);
                    nameField.setText("Optional");
                }
            }
        });
        c.add(nameField);

        descriptionLabel = new JLabel("Description : ");
        descriptionLabel.setSize(100, 30);
        descriptionLabel.setLocation(50, 115);
        c.add(descriptionLabel);

        description = new JTextArea();
        description.setLineWrap(false);
        description.setText("Optional");
        description.setForeground(Color.GRAY);
        description.setEnabled(true);
        description.setMargin(new Insets(4, 8, 2, 4));
        description.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                description.setForeground(UIManager.getColor("TextArea.foreground"));
                if(description.getText().equals("Optional")) {
                    description.setText("");
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (description.getText().length() == 0) {
                    description.setForeground(Color.GRAY);
                    description.setText("Optional");
                }
            }
        });
        description.setEditable(true);
        description.setToolTipText("Optional");

        JBScrollPane scroll = new JBScrollPane(description,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        scroll.setSize(400, 160);
        scroll.setLocation(50, 115+28);
        c.add(scroll);

        assertionLabel = new JLabel("Assertion : ");
        assertionLabel.setSize(100, 30);
        assertionLabel.setLocation(50, 325);
        c.add(assertionLabel);

        b1 = new JRadioButton("Assert Equals");
        b1.setActionCommand("Assert Equals");
        b1.setSize(150,30);
        b1.setLocation(150,325);
        c.add(b1);
        radioGroup.add(b1);

        b2 = new JRadioButton("Assert Not Equals");
        b2.setActionCommand("Assert Not Equals");
        b2.setSize(150,30);
        b2.setLocation(300,325);
        c.add(b2);
        radioGroup.add(b2);

        b1.setSelected(true);

        JLabel infoLabel = new JLabel("Case will be saved here : ");
        infoLabel.setFont(new Font("Verdana", Font.PLAIN, 12));
        infoLabel.setSize(400, 12);
        infoLabel.setLocation(50, 360);
        c.add(infoLabel);

        infoLabel = new JLabel(formatLocation(listener.getSaveLocation()));
        infoLabel.setFont(new Font("Verdana", Font.PLAIN, 12));
        infoLabel.setSize(400, 15);
        infoLabel.setLocation(50, 375);
        c.add(infoLabel);

        saveButton = new JButton("Save and Close");
        saveButton.setSize(150, 30);
        saveButton.setLocation(305, 410);
        saveButton.setIcon(UIUtils.SAVE_CANDIDATE_GREY);
        saveButton.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            triggerSave();
        }
        });
        c.add(saveButton);

        cancelButton = new JButton("Cancel");
        cancelButton.setSize(100, 30);
        cancelButton.setLocation(200, 410);
        cancelButton.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            form.dispose();
        }
        });
        c.add(cancelButton);
    }

    private void triggerSave()
    {
        String name_text = prepareString(nameField.getText());
        String description_text = prepareString(description.getText());
        ButtonModel model = radioGroup.getSelection();
        StoredCandidate.AssertionType type = StoredCandidate.AssertionType.EQUAL;
        if(model.getActionCommand().equals("Assert Not Equals"))
        {
            type = StoredCandidate.AssertionType.NOT_EQUAL;
        }
        this.listener.triggerRecordAddition(name_text,description_text,type);
        this.dispose();
    }

    private String prepareString(String source)
    {
        if(source.equals("Optional"))
        {
            return "";
        }
        else
        {
            return source.trim();
        }
    }

    public void setStoredCandidate(StoredCandidate storedCandidate)
    {
        this.storedCandidate = storedCandidate;
        setInfo();
    }

    private void setInfo()
    {
        boolean updated=false;
        if(this.storedCandidate!=null)
        {
            String name = storedCandidate.getName();
            String description = storedCandidate.getDescription();
            StoredCandidate.AssertionType assertionType = storedCandidate.getAssertionType();

            if(name!=null)
            {
                this.nameField.setText(name);
                updated = true;
            }
            if(description!=null)
            {
                this.description.setText(description);
                updated = true;
            }
            if(assertionType!=null)
            {
                switch (assertionType)
                {
                    case EQUAL:
                        b1.setSelected(true);
                        updated=true;
                        break;
                    default:
                        b2.setSelected(true);
                        updated = true;
                }
            }
            if(updated)
            {
                saveButton.setText("Update");
            }
        }
    }

    private String formatLocation(String location)
    {
        if(location.length()<=59)
        {
            return location;
        }
        else
        {
            String left = location.substring(0,47);
            left = left.substring(0,left.lastIndexOf("/")+1);
            left+=".../.unlogged/";
            return left;
        }
    }
}
