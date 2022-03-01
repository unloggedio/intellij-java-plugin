package com.insidious.plugin.ui;

import com.insidious.plugin.callbacks.SignUpCallback;
import com.insidious.plugin.util.LoggerUtil;
import org.slf4j.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.insidious.plugin.factory.InsidiousService;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class CredentialsToolbar {

    private static final Logger logger = LoggerUtil.getInstance(CredentialsToolbar.class);
    private final InsidiousService insidiousService;
    String usernameText;
    String videobugURL, passwordText;
    Project project;
    String projectName, project_id;
    ToolWindow toolWindow;
    private JPanel panel1;
    private JTextField username;
    private JLabel usernameLable;
    private JLabel passwordLable;
    private JPasswordField password;
    private JTextField videobugServerURLTextField;
    private JButton Signin;
    private JLabel errorLable;
    private JLabel commandLabel;
    private JTextArea textArea1;
    private JButton downloadAgent;
    private JButton Signup;
    private JLabel infoError;

    public CredentialsToolbar(Project project, ToolWindow toolWindow) {
        this.project = project;
        this.insidiousService = project.getService(InsidiousService.class);
        this.toolWindow = toolWindow;
        Signin.addActionListener(new ActionListener() {
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
        downloadAgent.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    java.awt.Desktop.getDesktop().browse(java.net.URI.create("https://drive.google.com/uc?id=1ZoNvBSSwMRIGwmit33WfVfkWh3-DyR56&export=download"));
                } catch (java.io.IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        });
        Signup.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                usernameText = username.getText();
                passwordText = new String(password.getPassword());
                videobugURL = videobugServerURLTextField.getText();
                insidiousService.signup(videobugURL, usernameText, passwordText, new SignUpCallback() {
                    @Override
                    public void error(String string) {

                    }

                    @Override
                    public void success() {
                        try {
                            insidiousService.signin(videobugURL, usernameText, passwordText);
                            infoError.setText("Signup was successful!");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    public void setErrorLabel(String message) {
        errorLable.setText(message);
    }

    public JPanel getContent() {
        return panel1;
    }

    public void setText(String s) {
        textArea1.setText(s);
    }

    public void setErrorLable(String s) {
        infoError.setText(s);
    }
}
