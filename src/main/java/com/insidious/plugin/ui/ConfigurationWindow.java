package com.insidious.plugin.ui;

import com.insidious.plugin.callbacks.SignUpCallback;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConfigurationWindow {
    private static final Logger logger = LoggerUtil.getInstance(ConfigurationWindow.class);
    private final Project project;
    private final ToolWindow toolWindow;
    private JPanel panel1;
    private JTextField email;
    private JPasswordField password;
    private JButton signInButton;
    private JTextArea loginSupportTextArea;
    private JButton signUpButton;
    private JButton useOfflineLocalRecordingsButton;
    private JTextField serverEndpoint;
    private JLabel serverLabel;
    private JLabel passwordLabel;
    private JLabel emailLabel;
    private JButton logoutButton;
    private JButton downloadJavaAgentToButton;
    private JButton buySingleUserLicenseButton;
    private JButton uploadSessionToServer;
    private JButton reportIssuesButton;
    private JPanel supportPanel;
    private JPanel outputPanel;
    private JPanel buttonsPanel;
    private JTextField supportEmailAddress;
    private JButton getInTouchButton;

    private final ExecutorService backgroundThreadExecutor = Executors.newFixedThreadPool(5);


    public ConfigurationWindow(Project project, ToolWindow toolWindow) {
        this.project = project;
//        this.insidiousService = project.getService(InsidiousService.class);
        this.toolWindow = toolWindow;

        getInTouchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String emailValue = supportEmailAddress.getText();
                if (!project.getService(InsidiousService.class).isValidEmailAddress(emailValue)) {
                    Messages.showErrorDialog("Please enter a valid email address", "Videobug");
                    return;
                }
                JSONObject eventProperties = new JSONObject();
                eventProperties.put("email", emailValue);
                UsageInsightTracker.getInstance().RecordEvent("RequestSupport", eventProperties);
                Messages.showMessageDialog("Someone from videobug will get in touch with you soon.", "Videobug", Messages.getInformationIcon());
                project.getService(InsidiousService.class).generateAndUploadReport();

            }
        });

    }

    public void setErrorLabel(String message) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Messages.showInfoMessage(project, message, "Error");
        });
    }

    public JPanel getContent() {
        return panel1;
    }

    public void setText(String s) {
        loginSupportTextArea.setText(s + "\n");
    }
}
