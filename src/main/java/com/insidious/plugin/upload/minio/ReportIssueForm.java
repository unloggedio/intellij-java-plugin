package com.insidious.plugin.upload.minio;// Java program to implement
// a Simple Registration Form
// using Java Swing

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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

        setTitle("Registration Form");
        setBounds(400, 150, 800, 600);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        c = getContentPane();
        c.setLayout(null);

        title = new JLabel("Report Issue");
        title.setFont(new Font("Sans Serif", Font.PLAIN, 24));
        title.setSize(300, 30);
        title.setLocation(300, 30);
        c.add(title);

        errorText = new JLabel();
        errorText.setFont(new Font("Sans Serif", Font.PLAIN, 12));
        errorText.setLocation(300, 50);
        errorText.setSize(150, 30);
        c.add(errorText);

        userEmailLabel = new JLabel("Email");
        userEmailLabel.setFont(new Font("Sans Serif", Font.PLAIN, 16));
        userEmailLabel.setSize(800, 30);
        userEmailLabel.setLocation(100, 100);
        c.add(userEmailLabel);

        userEmail = new JTextField();
        userEmail.setFont(new Font("Sans Serif", Font.PLAIN, 16));
        userEmail.setSize(250, 30);
        userEmail.setLocation(200, 100);
        c.add(userEmail);

        issueTitleLabel = new JLabel("Issue Title");
        issueTitleLabel.setFont(new Font("Sans Serif", Font.PLAIN, 16));
        issueTitleLabel.setSize(100, 30);
        issueTitleLabel.setLocation(100, 150);
        c.add(issueTitleLabel);

        issueTitle = new JTextField();
        issueTitle.setFont(new Font("Sans Serif", Font.PLAIN, 16));
        issueTitle.setSize(300, 30);
        issueTitle.setLocation(200, 150);
        c.add(issueTitle);

        descriptionLabel = new JLabel("Description");
        descriptionLabel.setFont(new Font("Sans Serif", Font.PLAIN, 16));
        descriptionLabel.setSize(100, 30);
        descriptionLabel.setLocation(100, 200);
        c.add(descriptionLabel);

        description = new JTextArea();
        description.setFont(new Font("Sans Serif", Font.PLAIN, 16));
        description.setSize(500, 200);
        description.setLocation(200, 200);
        description.setLineWrap(true);
        description.setEnabled(true);
        c.add(description);

//        scroll = new JBScrollPane(description,
//                JBScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JBScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
//        c.add(scroll);

        submitButton = new JButton("Send Email");
        submitButton.setFont(new Font("Sans Serif", Font.BOLD, 15));
        submitButton.setSize(150, 40);
        submitButton.setLocation(400, 440);
        submitButton.addActionListener(this);
        c.add(submitButton);

//        setVisible(true);
    }

    private boolean validateForm() {
        StringBuilder errorText = new StringBuilder();
        if (userEmail.getText().length() <= 5 || !userEmail.getText().contains("@")) {
            errorText.append("Email is mandatory\n");
            userEmail.setBackground(Color.red);
        }

        if (issueTitle.getText().length() < 5) {
            errorText.append("Issue Title is mandatory");
            issueTitle.setBackground(Color.red);
        }

        return errorText.length() == 0;
    }

    public void actionPerformed(ActionEvent e) {
        if (!validateForm()) {
            return;
        }

        ReportIssue reportIssue = new ReportIssue();

        File selogDir = new File(reportIssue.getLatestSeLogFolderPath());
        String sessionObjectKey = userEmail.getText() + "/" + selogDir.getName() + ".zip";
        String ideaLogObjectKey = userEmail.getText() + "/idea-" + selogDir.getName() + ".log";

        String sessionURI = FileUploader.ENDPOINT + "/" + FileUploader.BUCKET_NAME + "/" + sessionObjectKey;
        String ideaLogURI = FileUploader.ENDPOINT + "/" + FileUploader.BUCKET_NAME + "/" + ideaLogObjectKey;

        String issueDescription = "Issue Raised by: `" + userEmail.getText() + "`\n\n"
                + "[Session Logs](" + sessionURI + ")" + "\n\n" + "[idea.log file](" + ideaLogURI + ") \n\n" + description.getText();

        Desktop desktop = Desktop.getDesktop();
        String gitlabMail = "contact-project+insidious1-server-32379664-issue-@incoming.gitlab.com";

        String mailFromBrowser = "https://mail.google.com/mail/?view=cm&fs=1&to=" + URLEncoder.encode(gitlabMail, StandardCharsets.UTF_8).replace("+", "%20")
                + "&su=" + URLEncoder.encode(issueTitle.getText(), StandardCharsets.UTF_8).replace("+", "%20")
                + "&body=" + URLEncoder.encode(issueDescription, StandardCharsets.UTF_8).replace("+", "%20");

        // for using from machine app
//                        String message2 = "mailto:contact-project+insidious1-server-32379664-issue-@incoming.gitlab.com?subject=" +
//                                URLEncoder.encode(issueTitle, StandardCharsets.UTF_8).replace("+", "%20") +
//                                "&body=" + URLEncoder.encode(issueDescription, StandardCharsets.UTF_8).replace("+", "%20");

        URI uri = URI.create(mailFromBrowser);
        System.out.print(mailFromBrowser);

        try {
            desktop.browse(uri);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        ProgressManager.getInstance().run(reportIssue.zippingAndUploadTask(project, sessionObjectKey, ideaLogObjectKey));
    }
}