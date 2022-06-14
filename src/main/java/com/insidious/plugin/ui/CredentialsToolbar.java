package com.insidious.plugin.ui;

import com.insidious.plugin.callbacks.SignUpCallback;
import com.insidious.plugin.client.VideobugLocalClient;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CredentialsToolbar {
    private static final Logger logger = LoggerUtil.getInstance(CredentialsToolbar.class);
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
    private JButton uploadSessionToServer;
    private final InsidiousService insidiousService;
    private final ExecutorService backgroundThreadExecutor = Executors.newFixedThreadPool(5);


    public CredentialsToolbar(Project project, ToolWindow toolWindow) {
        this.project = project;
        this.insidiousService = project.getService(InsidiousService.class);
        this.toolWindow = toolWindow;

        email.setText(this.insidiousService.getConfiguration().username);
        if (!"test@example.com".equals(this.insidiousService.getConfiguration().username)) {
            password.setText("");
        }
        serverEndpoint.setText(this.insidiousService.getConfiguration().serverUrl);

        useOfflineLocalRecordingsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                insidiousService.initiateUseLocal();
//                if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
//                }
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

        downloadJavaAgentToButton.addActionListener(ae -> insidiousService.ensureAgentJar());

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
                                loginSupportTextArea.setText("Failed to signup\n" + string);
                                Notifications.Bus.notify(
                                        InsidiousNotification.balloonNotificationGroup
                                                .createNotification(
                                                        "Failed to signup - " + string, NotificationType.ERROR), project);

                            }

                            @Override
                            public void success() {
                                try {
                                    insidiousService.signin(videobugURL, usernameText, passwordText);
                                    loginSupportTextArea.setText("\nSignup was successful!");
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

        uploadSessionToServer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (insidiousService.getClient() instanceof VideobugLocalClient) {
                    InsidiousNotification.notifyMessage(
                            "Please login to a server for uploading session",
                            NotificationType.ERROR
                    );
                    return;
                }

                try {
                    insidiousService.uploadSessionToServer();
                } catch (IOException ex) {
                    InsidiousNotification.notifyMessage(
                            "Failed to upload archives to server - " + ex.getMessage(),
                            NotificationType.ERROR
                    );
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
        loginSupportTextArea.setText(s + "\n");
    }
}
