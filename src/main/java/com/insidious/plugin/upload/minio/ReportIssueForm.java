package com.insidious.plugin.upload.minio;// Java program to implement
// a Simple Registration Form
// using Java Swing

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import net.openhft.chronicle.core.util.Time;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ReportIssueForm extends JFrame implements ActionListener {

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
    private Project project;

    public ReportIssueForm(Project project) {
        this.project = project;

        setTitle("Unlogged Inc.");
        setBounds(400, 150, 750, 600);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        c = getContentPane();
        c.setLayout(null);

        title = new JLabel("Report Issue");
        title.setFont(new Font("", Font.PLAIN, 24));
        title.setSize(300, 30);
        title.setLocation(300, 30);
        c.add(title);

        errorText = new JLabel();
        errorText.setLocation(300, 50);
        errorText.setSize(150, 30);
        c.add(errorText);

        userEmailLabel = new JLabel("Email");
        userEmailLabel.setSize(100, 30);
        userEmailLabel.setLocation(50, 80);
        c.add(userEmailLabel);

        userEmail = new JTextField();
        userEmail.setSize(300, 30);
        userEmail.setLocation(150, 80);
        c.add(userEmail);

        issueTitleLabel = new JLabel("Issue Title");
        issueTitleLabel.setSize(100, 30);
        issueTitleLabel.setLocation(50, 130);
        c.add(issueTitleLabel);

        issueTitle = new JTextField();
        issueTitle.setSize(300, 30);
        issueTitle.setLocation(150, 130);
        issueTitle.setToolTipText("The generated test");
        c.add(issueTitle);

        List<String> checkboxes = new ArrayList<>();
        checkboxes.add("Test Fails on running");
        checkboxes.add("Test doesn't compile");
        checkboxes.add("Generate Test button fails");
        checkboxes.add("Wrong Class of variable");
        checkboxes.add("Other");

        int x = 50;
        int y = 150;
        int d = 0;

        for (int i = 0; i < checkboxes.size(); i++) {
            JCheckBox checkBox = new JCheckBox();
            checkBox.setSize(180, 30);
            checkBox.setText(checkboxes.get(i));

            if (i % 3 == 0) {
                y += 30;
                d = 0;
            }
            checkBox.setLocation(x + d, y);
            d += 200;
            c.add(checkBox);
        }

        descriptionLabel = new JLabel("Description");
        descriptionLabel.setSize(100, 30);
        descriptionLabel.setLocation(50, 250);
        c.add(descriptionLabel);

        description = new JTextArea();
        description.setLineWrap(false);
        description.setEnabled(true);
        description.setEditable(true);
        description.setToolTipText("Add Stacktrace, sample code which can help us recreate the issue");

        JBScrollPane scroll = new JBScrollPane(description,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        scroll.setSize(550, 250);
        scroll.setLocation(150, 250);
        c.add(scroll);

        submitButton = new JButton("Send Email");
        submitButton.setSize(150, 30);
        submitButton.setLocation(550, 510);
        submitButton.addActionListener(this);
        c.add(submitButton);
    }

    private boolean validateForm() {
        StringBuilder errorText = new StringBuilder();
        if (userEmail.getText().length() <= 5 || !userEmail.getText().contains("@")) {
            errorText.append("Email is mandatory\n");
            userEmail.setBackground(Color.red);
        }

        return errorText.length() == 0;
    }

    public void actionPerformed(ActionEvent e) {
        if (!validateForm()) {
            return;
        }

        ReportIssue reportIssue = new ReportIssue();

        File selogDir = new File(reportIssue.getLatestSeLogFolderPath());

        String s3BucketParentPath = userEmail.getText() + "/" + selogDir.getName() + "-" + Time.uniqueId();
        String sessionObjectKey = s3BucketParentPath + "/" + selogDir.getName() + ".zip";

        System.out.println(sessionObjectKey);

        String sessionURI = FileUploader.ENDPOINT + "/" + FileUploader.BUCKET_NAME + "/" + sessionObjectKey;

        String issueDescription = "Issue Raised by: `" + userEmail.getText() + "`\n\n"
                + "[Session Logs](" + sessionURI.replace("+", "%2B").replace("@", "%40") + ")" + "\n\n" + description.getText();

        Desktop desktop = Desktop.getDesktop();
        String gitlabMail = "contact-project+insidious1-server-32379664-issue-@incoming.gitlab.com";

        String mailFromBrowser = "https://mail.google.com/mail/?view=cm&fs=1&to=" + URLEncoder.encode(gitlabMail, StandardCharsets.UTF_8).replace("+", "%20")
                + "&su=" + URLEncoder.encode(issueTitle.getText(), StandardCharsets.UTF_8).replace("+", "%20")
                + "&body=" + URLEncoder.encode(issueDescription, StandardCharsets.UTF_8).replace("+", "%20");

        URI uri = URI.create(mailFromBrowser);

        try {
            desktop.browse(uri);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        ProgressManager.getInstance().run(reportIssue.zippingAndUploadTask(project, sessionObjectKey));
    }
}