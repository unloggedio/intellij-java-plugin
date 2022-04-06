package com.insidious.plugin.ui;

import com.insidious.plugin.callbacks.SignUpCallback;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import org.slf4j.Logger;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CredentialsToolbar {

    private static final Logger logger = LoggerUtil.getInstance(CredentialsToolbar.class);
    private final InsidiousService insidiousService;
    private final ExecutorService backgroundThreadExecutor = Executors.newFixedThreadPool(5);
    String usernameText;
    String videobugURL, passwordText;
    Project project;
    ToolWindow toolWindow;
    private JPanel panel1;
    private JTextField username;
    private JLabel usernameLabel;
    private JLabel passwordLabel;
    private JPasswordField password;
    private JTextField videobugServerURLTextField;
    private JButton signinButton;
    private JLabel errorLabel;
    private JLabel commandLabel;
    private JTextArea textArea1;
    private JButton downloadAgent;
    private JButton signupButton;
    private JLabel infoError;
    private JButton logoutButton;
    private JButton payment;

    public CredentialsToolbar(Project project, ToolWindow toolWindow) {
        this.project = project;
        this.insidiousService = project.getService(InsidiousService.class);
        this.toolWindow = toolWindow;

        username.setText(this.insidiousService.getConfiguration().username);
        if (!"test@example.com".equals(this.insidiousService.getConfiguration().username)) {
            password.setText("");
        }
        videobugServerURLTextField.setText(this.insidiousService.getConfiguration().serverUrl);

        signinButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                usernameText = username.getText();
                passwordText = new String(password.getPassword());
                videobugURL = videobugServerURLTextField.getText();
                try {
                    insidiousService.signin(videobugURL, usernameText, passwordText);
                } catch (IOException e) {
                    e.printStackTrace();
                    Messages.showErrorDialog(project, "Couldn't connect with server - " + e.getMessage(), "Failed");
                }
            }
        });
        textArea1.setLineWrap(true);

        logoutButton.addActionListener(e -> {
            insidiousService.logout();
            username.setText("");
            password.setText("");
            videobugServerURLTextField.setText(
                    insidiousService.getConfiguration().getDefaultCloudServerUrl());
            textArea1.setText("");
        });

        downloadAgent.addActionListener(ae -> insidiousService.downloadAgent());

        signupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                usernameText = username.getText();
                passwordText = new String(password.getPassword());
                videobugURL = videobugServerURLTextField.getText();
                insidiousService.signup(videobugURL, usernameText, passwordText,
                        new SignUpCallback() {
                            @Override
                            public void error(String string) {
                                Notifications.Bus.notify(
                                        insidiousService.getNotificationGroup()
                                                .createNotification(
                                                        "Failed to signup - " + string,
                                                        NotificationType.ERROR),
                                        project);

                            }

                            @Override
                            public void success() {
                                try {
                                    insidiousService.signin(videobugURL, usernameText, passwordText);
                                    infoError.setText("Signup was successful!");
                                    ReadAction.nonBlocking(insidiousService::checkAndEnsureJavaAgentCache)
                                            .submit(backgroundThreadExecutor);
                                    ReadAction.nonBlocking(insidiousService::identifyTargetJar)
                                            .submit(backgroundThreadExecutor);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
            }
        });
        payment.addActionListener(new ActionListener() {
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
        Messages.showInfoMessage(project, message, "Error");
    }

    public JPanel getContent() {
        return panel1;
    }

    public void setText(String s) {
        textArea1.setText(s);
    }

    public void setInfoLabel(String s) {
        infoError.setText(s);
    }
}
