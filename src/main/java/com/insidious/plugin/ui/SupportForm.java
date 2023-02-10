package com.insidious.plugin.ui;

import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.upload.minio.ReportIssue;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.regex.Pattern;

public class SupportForm {
    private JPanel mainPanel;
    private JPanel supportForm;
    private JPanel issueSelector;
    private JComboBox issueSelectorComboBox;
    private JLabel issueSelectorLabel;
    private JTextField emailTextField;
    private JLabel emailLabel;
    private JTextArea customMessageArea;
    private JLabel yourMessageLabel;
    private JPanel buttonGroup;
    private JButton submitButton;
    private JButton discordButton;
    private Project project;
    public SupportForm(Project project)
    {
        this.project = project;
        this.submitButton.setIcon(UIUtils.CHECK_MARK_SVG);
        submitButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                uploadLogsAndSendMail();
            }
        });
        discordButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                routeToDiscord();
            }
        });
    }
    public JComponent getComponent()
    {
        return mainPanel;
    }

    public void uploadLogsAndSendMail()
    {
        if(!areInputsValid())
        {
            return;
        }
        JSONObject eventProperties = new JSONObject();
        eventProperties.put("email", this.emailTextField.getText().trim());
        eventProperties.put("issue_type", this.issueSelectorComboBox.getSelectedItem().toString());
        eventProperties.put("issue_text", this.customMessageArea.getText().toString());
        UsageInsightTracker.getInstance()
                .RecordEvent("SupportIssueRaised", eventProperties);

        ReportIssue reportIssue = new ReportIssue();
        ProgressManager.getInstance().run(
                reportIssue.sendSupportMessage(project,
                        this.emailTextField.getText().trim(),
                        this.issueSelectorComboBox.getSelectedItem().toString(),
                        this.customMessageArea.getText().trim()));
    }

    private boolean areInputsValid()
    {
        String email = this.emailTextField.getText().trim();
        boolean emailValid = Pattern.compile("^(.+)@(\\S+)$")
                .matcher(email)
                .matches();
        if(!emailValid)
        {
            InsidiousNotification.notifyMessage("Invalid email format.",
                    NotificationType.ERROR);
            return false;
        }
        String text_description = this.customMessageArea.getText().trim();
        if(text_description.length()==0)
        {
            InsidiousNotification.notifyMessage("Please provide a description for the issue faced.",
                    NotificationType.ERROR);
            return false;
        }
        return true;
    }

    private void routeToDiscord() {
        String link = "https://discord.gg/Hhwvay8uTa";
        if (Desktop.isDesktopSupported()) {
            try {
                java.awt.Desktop.getDesktop()
                        .browse(java.net.URI.create(link));
            } catch (Exception e) {
            }
        }
    }
}
