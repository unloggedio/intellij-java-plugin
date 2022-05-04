package com.insidious.plugin.ui;

import com.insidious.plugin.callbacks.SignUpCallback;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import org.slf4j.Logger;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CredentialsToolbar {
    private final Project project;
    private final ToolWindow toolWindow;
    private JPanel panel1;
    private JTextField email;
    private JPasswordField password;
    private JButton signInButton;
    private JTextArea loginSupportTextArea;
    private JPanel loginFormPanel;
    private JButton signUpButton;
    private JButton useOfflineLocalRecordingsButton;
    private JTextField serverEndpoint;
    private JLabel serverLabel;
    private JLabel passwordLabel;
    private JLabel emailLabel;
    private JButton logoutButton;
    private JButton downloadJavaAgentToButton;
    private JButton buySingleUserLicenseButton;

    private static final Logger logger = LoggerUtil.getInstance(CredentialsToolbar.class);
    private InsidiousService insidiousService;
    private  ExecutorService backgroundThreadExecutor = Executors.newFixedThreadPool(5);


    public CredentialsToolbar(Project project, ToolWindow toolWindow) {
        this.project = project;
        this.insidiousService = project.getService(InsidiousService.class);
        this.toolWindow = toolWindow;

        email.setText(this.insidiousService.getConfiguration().username);
        if (!"test@example.com".equals(this.insidiousService.getConfiguration().username)) {
            password.setText("");
        }
        serverEndpoint.setText(this.insidiousService.getConfiguration().serverUrl);

        useOfflineLocalRecordingsButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    insidiousService.initiateUseLocal();
                }
            }
        });

        signInButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String usernameText = email.getText();
                String passwordText = new String(password.getPassword());
                String videobugURL = serverEndpoint.getText();
                try {
                    insidiousService.signin(videobugURL, usernameText, passwordText);
                } catch (IOException e) {
                    e.printStackTrace();
                    Messages.showErrorDialog(project, "Couldn't connect with server - " + e.getMessage(), "Failed");
                }
            }
        });
        loginSupportTextArea.setLineWrap(true);

        logoutButton.addActionListener(e -> {
            insidiousService.logout();
            email.setText("");
            password.setText("");
            serverEndpoint.setText(
                    insidiousService.getConfiguration().getDefaultCloudServerUrl());
            loginSupportTextArea.setText("");
        });

        downloadJavaAgentToButton.addActionListener(ae -> insidiousService.downloadAgent());

        signUpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String usernameText = email.getText();
                String passwordText = new String(password.getPassword());
                String videobugURL = serverEndpoint.getText();
                insidiousService.signup(videobugURL, usernameText, passwordText,
                        new SignUpCallback() {
                            @Override
                            public void error(String string) {
                                Notifications.Bus.notify(
                                        insidiousService.getNotificationGroup()
                                                .createNotification(
                                                        "Failed to signup - " + string, NotificationType.ERROR), project);

                            }

                            @Override
                            public void success() {
                                try {
                                    insidiousService.signin(videobugURL, usernameText, passwordText);
                                    loginSupportTextArea.append("\nSignup was successful!");
                                    ReadAction.nonBlocking(insidiousService::checkAndEnsureJavaAgentCache)
                                            .submit(backgroundThreadExecutor);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
            }
        });
        buySingleUserLicenseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    java.awt.Desktop.getDesktop().browse(java.net.URI.create("https://buy.stripe.com/7sIeUU7KU2LK2FW4gg"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
        loginSupportTextArea.append(s + "\n");
    }
}
