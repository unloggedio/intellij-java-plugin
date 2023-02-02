package com.insidious.plugin.ui.Components;

import com.insidious.plugin.pojo.ProjectTypeInfo;
import com.insidious.plugin.ui.UIUtils;

import javax.swing.*;
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
    private CardActionListener listener;

    private boolean logsPresent = false;

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
//        if(defaultType.equals(ProjectTypeInfo.RUN_TYPES.INTELLIJ_APPLICATION))
//        {
//            checkIfRunnable();
//        }
//        else
//        {
//            hideRunButton();
//        }
        waitingForLogs.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleTransition();
            }
        });
    }

    void checkIfRunnable() {
        if (listener.hasRunnableApplicationConfig()) {
            this.descriptionText_Area.setText("" +
                    "We have found a run configuration, click on Run with unlogged.</p>" +
                    "Once you run the application with the agent, access your application from Postman, Swagger or UI and Unlogged will be ready to start generating the unit tests.</p>" +
                    "");
            this.runWithUnlogged.setVisible(true);
//            if(listener.isApplicationRunning())
//            {
//                setRunButtonState(false);
//            }
//           else
//            {
            runWithUnlogged.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    triggerRunWithUnlogged();
                }
            });
//            }
        } else {
            hideRunButton();
        }
    }

    void hideRunButton() {
        this.runWithUnlogged.setVisible(false);
    }

    void triggerRunWithUnlogged() {
        listener.runApplicationWithUnlogged();
//        if(listener.runApplicationWithUnlogged())
//        {
//            setRunButtonState(false);
//        }
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

    private void setRunButtonState(boolean status) {
        if (!status) {
            this.runWithUnlogged.setBorderPainted(true);
            this.runWithUnlogged.setOpaque(false);
            this.runWithUnlogged.setContentAreaFilled(false);
        } else {
            this.runWithUnlogged.setBorderPainted(false);
            this.runWithUnlogged.setOpaque(true);
            this.runWithUnlogged.setContentAreaFilled(false);
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
}
