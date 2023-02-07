package com.insidious.plugin.ui.Components;

import com.insidious.plugin.pojo.ProjectTypeInfo;
import com.insidious.plugin.ui.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Run_Component_Obv3 {
    private JPanel mainPanel;
    private JTextArea VMoptionsArea;
    private JButton runWithUnlogged;
    private JButton waitingForLogs;
    private JLabel headingLabel;
    private JTextArea descriptionText_Area;
    private JPanel InstructionsPanel;
    private JPanel GifPanel;
    private JScrollPane scrollParent;
    private JPanel MainContent;
    private JPanel textAreaParent;
    private JLabel iconHolder;
    private JTextField baseLabelTextField;
    private JLabel basePackageLabel;
    private JPanel basePackageSelectionPanel;
    private CardActionListener listener;
    private boolean logsPresent = false;

    private String fallbackPackage;

    public Run_Component_Obv3(ProjectTypeInfo.RUN_TYPES defaultType, boolean logsPresent, CardActionListener listener) {
        this.listener = listener;
        this.headingLabel.setText("Run Unlogged with " + UIUtils.getDisplayNameForType(defaultType));
        this.headingLabel.setIcon(UIUtils.getIconForRuntype(defaultType));
        setGifIconForType(defaultType);
        setDescriptionText(defaultType);
        if (logsPresent) {
            this.logsPresent = logsPresent;
            setWaitingButtonReadyState();
        } else {
            listener.checkForSelogs();
        }
        waitingForLogs.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleTransition();
            }
        });
        baseLabelTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                UpdateBasePackage();
            }
            public void removeUpdate(DocumentEvent e) {
                UpdateBasePackage();
            }
            public void insertUpdate(DocumentEvent e) {
                UpdateBasePackage();
            }

            private void UpdateBasePackage() {
                try
                {
                    if(baseLabelTextField.getText()!=null && !baseLabelTextField.getText().equals("Label")) {
                            updateVmOptionsBasePackage(baseLabelTextField.getText());
                    }
                }
                catch (IllegalStateException e)
                {
                    //nothing to do
                }
                catch (Exception e)
                {
                    setBasePackageText(fallbackPackage);
                }
            }
        });
    }

    void handleTransition() {
        if (logsPresent) {
            listener.loadLiveLiew();
        }
    }

    public JPanel getComponent() {
        return mainPanel;
    }

    public void setVMtext(String text) {
        this.VMoptionsArea.setText(text);
    }

    public void updateVmOptionsBasePackage(String packageString)
    {
        listener.updateBasePackage(packageString);
    }

    private void setDescriptionText(ProjectTypeInfo.RUN_TYPES type) {
        if (type.equals(ProjectTypeInfo.RUN_TYPES.INTELLIJ_APPLICATION)) {
            this.descriptionText_Area.setText("" +
                    "Add these VM parameters into your run configuration and start your application.\n" +
                    "\nOnce you run the application with the agent, access your application from Postman, Swagger or UI and Unlogged will be ready to start generating the unit tests.\n" +
                    "");
        } else if (type.equals(ProjectTypeInfo.RUN_TYPES.GRADLE_CLI)) {
            this.descriptionText_Area.setText("" +
                    "Add these VM paramters into your build.gradle and start your application.\n" +
                    "\nOnce you run the application with the agent, access your application from Postman, Swagger or UI and Unlogged will be ready to start generating the unit tests.\n" +
                    "");
        } else {
            this.descriptionText_Area.setText("" +
                    "Copy this command and run inside your terminal so that your application starts running with our agent.\n" +
                    "\nOnce you run the application with the agent, access your application from Postman, Swagger or UI and Unlogged will be ready to start generating the unit tests.\n" +
                    "");
        }
    }

    void setWaitingButtonReadyState() {
        waitingForLogs.setText("Proceed to generate cases");
        waitingForLogs.setIcon(UIUtils.GENERATE_ICON);
        waitingForLogs.setBorderPainted(false);
        waitingForLogs.setContentAreaFilled(false);
        waitingForLogs.setOpaque(true);
        waitingForLogs.setBackground(UIUtils.green);
        waitingForLogs.setForeground(Color.white);
    }

    void setGifIconForType(ProjectTypeInfo.RUN_TYPES type) {
        switch (type) {
            case INTELLIJ_APPLICATION:
                UIUtils.setGifIconForLabel(this.iconHolder, "intellij_run.gif", null);
                break;
            case GRADLE_CLI:
                UIUtils.setGifIconForLabel(this.iconHolder, "gradle_run.gif", null);
                break;
            case MAVEN_CLI:
                UIUtils.setGifIconForLabel(this.iconHolder, "maven_cli.gif", null);
                break;
            case JAVA_JAR_CLI:
                UIUtils.setGifIconForLabel(this.iconHolder, "java_jar_cmd.gif", null);
                break;
        }
    }

    public void setBasePackageText(String basePackage) {
        this.baseLabelTextField.setText(basePackage);
    }

    public void setFallbackPackage(String fallbackPackage) {
        this.fallbackPackage = fallbackPackage;
    }

    public String getFallbackPackage() {
        return fallbackPackage;
    }
}
