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
    private JButton generateTestCases;
    private final InsidiousService insidiousService;
    private final ExecutorService backgroundThreadExecutor = Executors.newFixedThreadPool(5);


    public ConfigurationWindow(Project project, ToolWindow toolWindow) {
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

        generateTestCases.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {

                    @Nullable String classNamesList =
                            Messages.showInputDialog("Class names list", "Videobug", null);
                    if (classNamesList == null || classNamesList.length() == 0) {
                        return;
                    }

                    insidiousService.generateTestCases(Arrays.asList(classNamesList.split(",")));
                } catch (InterruptedException | APICallException | IOException ex) {
                    throw new RuntimeException(ex);
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

//        logoutButton.addActionListener(e -> {
//            insidiousService.logout();
//            email.setText("");
//            password.setText("");
//            serverEndpoint.setText(insidiousService.getConfiguration().getDefaultCloudServerUrl());
//            loginSupportTextArea.setText("");
//        });

        downloadJavaAgentToButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                InsidiousNotification.notifyMessage(
                        "Downloading videobug java agent to $HOME/.videobug/videobug-java-agent.jar. Please wait for the download to complete.",
                        NotificationType.INFORMATION
                );
                insidiousService.ensureAgentJar(true);
            }
        });

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
//        buySingleUserLicenseButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent actionEvent) {
//                try {
//                    java.awt.Desktop.getDesktop().browse(java.net.URI.create("https://buy.stripe.com/7sIeUU7KU2LK2FW4gg"));
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        });

//        reportIssuesButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//               insidiousService.generateAndUploadReport();
//            }
//        });

        getInTouchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String emailValue = supportEmailAddress.getText();
                if (!insidiousService.isValidEmailAddress(emailValue)) {
                    Messages.showErrorDialog("Please enter a valid email address", "Videobug");
                    return;
                }
                JSONObject eventProperties = new JSONObject();
                eventProperties.put("email", emailValue);
                UsageInsightTracker.getInstance().RecordEvent("RequestSupport", eventProperties);
                Messages.showMessageDialog("Someone from videobug will get in touch with you soon.", "Videobug", Messages.getInformationIcon());
                insidiousService.generateAndUploadReport();

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
