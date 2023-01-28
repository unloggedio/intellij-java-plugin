package com.insidious.plugin.upload.minio;// Java program to implement
// a Simple Registration Form
// using Java Swing

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

public class ReportIssueForm extends JFrame implements ActionListener {

    private final String HINT_TEXT = "// My Test Case generation fails for this method \n\n"
            + "```java\n"
            + "public class UserRepo {\n"
            + "   public Optional<User> findUserInDB(long id){\n"
            + "      String queryString  = \"SELECT * FROM users WHERE id=($1)\";\n"
            + "      return dbConn.query(queryString, id);\n"
            + "   }\n"
            + "}\n"
            + "```\n";

    private Map<String, Boolean> checkboxes = new HashMap<>();
    private Container c;
    private JLabel title;
    private JLabel userEmailLabel;
    private JTextField userEmail;
    private JLabel issueTitleLabel;
    private JTextField issueTitle;
    private JLabel descriptionLabel;
    private JTextArea description;
    private JButton submitButton;
    private JScrollPane scroll;
    private JLabel errorText;
    private JLabel titleErrorText;
    private Project project;

    public ReportIssueForm(Project project) {
        this.project = project;

        setTitle("Unlogged Inc.");
        setBounds(300, 120, 750, 600);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        c = getContentPane();
        c.setLayout(null);

        title = new JLabel("Report Issue");
        title.setFont(new Font("", Font.PLAIN, 20));
        title.setSize(300, 30);
        title.setLocation(300, 15);
        c.add(title);

        errorText = new JLabel();
        errorText.setForeground(Color.red);
        errorText.setSize(150, 30);
        errorText.setLocation(170, 52);
        c.add(errorText);

        userEmailLabel = new JLabel("Email");
        userEmailLabel.setSize(100, 30);
        userEmailLabel.setLocation(50, 75);
        c.add(userEmailLabel);

        userEmail = new JTextField();
        userEmail.setSize(300, 30);
        userEmail.setLocation(120, 75);
        c.add(userEmail);

        titleErrorText = new JLabel();
        titleErrorText.setForeground(Color.red);
        titleErrorText.setSize(200, 30);
        titleErrorText.setLocation(170, 102);
        c.add(titleErrorText);

        issueTitleLabel = new JLabel("Issue Title");
        issueTitleLabel.setSize(100, 30);
        issueTitleLabel.setLocation(50, 125);
        c.add(issueTitleLabel);

        issueTitle = new JTextField();
        issueTitle.setSize(300, 30);
        issueTitle.setLocation(120, 125);
        issueTitle.setToolTipText("The generated test");
        c.add(issueTitle);

        int x = 120;
        int y = 135;
        int d = 0;

        checkboxes.put("Test Fails on running", false);
        checkboxes.put("Test doesn't compile", false);
        checkboxes.put("Generate Test button fails", false);
        checkboxes.put("Wrong Class of variable", false);
        checkboxes.put("Other", false);

        int i = 0;
        for (Map.Entry<String, Boolean> me : checkboxes.entrySet()) {
            JCheckBox checkBox = new JCheckBox();
            checkBox.setSize(180, 30);
            checkBox.setText(me.getKey());
            checkBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        JCheckBox item = (JCheckBox) e.getItem();
                        checkboxes.put(item.getText(), item.isSelected());
                    }
                }
            });

            if (i % 3 == 0) {
                y += 30;
                d = 0;
            }
            checkBox.setLocation(x + d, y);
            d += 200;
            c.add(checkBox);
            i++;
        }

        descriptionLabel = new JLabel("Description");
        descriptionLabel.setSize(100, 30);
        descriptionLabel.setLocation(48, 235);
        c.add(descriptionLabel);

        description = new JTextArea();
        description.setLineWrap(false);
        description.setText(HINT_TEXT);
        description.setForeground(Color.GRAY);
        description.setEnabled(true);
        description.setMargin(new Insets(4, 8, 2, 4));
        description.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                description.setForeground(UIManager.getColor("TextArea.foreground"));
                description.setText("");
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (description.getText().length() == 0) {
                    description.setForeground(Color.GRAY);
                    description.setText(HINT_TEXT);
                }
            }
        });
        description.setEditable(true);
        description.setToolTipText("Add Stacktrace, sample code which can help us recreate the issue");

        JBScrollPane scroll = new JBScrollPane(description,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        scroll.setSize(550, 250);
        scroll.setLocation(120, 235);
        c.add(scroll);

        JTextPane note = new JTextPane();
        note.setText("Note: On submitting this form we will upload session logs for debugging purpose.\n         After upload completion, you will be redirected to Gmail for finally submitting the report.");
        note.setFont(new Font("", Font.PLAIN, 12));
        note.setForeground(Color.GRAY);
        note.setLocation(120, 495);
        note.setSize(600, 30);
        c.add(note);

        submitButton = new JButton("Submit");
        submitButton.setSize(150, 30);
        submitButton.setLocation(530, 530);
        submitButton.addActionListener(this);
        c.add(submitButton);
    }

    private boolean validateForm() {
        if (userEmail.getText().length() <= 5 || !userEmail.getText().contains("@")) {
            errorText.setText("Email is mandatory\n");
            userEmail.setBackground(Color.RED);
        } else {
            userEmail.setBackground(UIManager.getColor("TextField:background"));
            errorText.setText("");
        }

        if (issueTitle.getText().length() <= 5) {
            titleErrorText.setText("Add a title to describe the issue");
            issueTitle.setBackground(Color.RED);
        } else {
            issueTitle.setBackground(UIManager.getColor("TextField:background"));
            titleErrorText.setText("");
        }

        return errorText.getText().length() == 0 && titleErrorText.getText().length() == 0;
    }

    public void actionPerformed(ActionEvent e) {
        if (!validateForm()) {
            return;
        }

        if (description.getText().equals(HINT_TEXT)) {
            description.setText("");
        }

        StringBuilder checkBoxLabel = new StringBuilder();

        for (Map.Entry<String, Boolean> me : checkboxes.entrySet()) {
            if (me.getValue()) {
                checkBoxLabel.append(me.getKey())
                        .append(", ");
            }
        }

        ReportIssue reportIssue = new ReportIssue();

        ProgressManager.getInstance().run(
                reportIssue.zippingAndUploadTask(project,
                        userEmail.getText(),
                        issueTitle.getText(),
                        description.getText(),
                        checkBoxLabel.toString()));

        setVisible(false);
    }
}