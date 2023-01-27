package com.insidious.plugin.ui.Components;

import com.insidious.plugin.pojo.ProjectTypeInfo;
import com.insidious.plugin.ui.UI_Utils;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Run_Component_Obv3 {
    private JPanel mainPanel;
    private JPanel borderPaent;
    private JPanel bottomPanel;
    private JPanel MainContent;
    private JPanel textAreaParent;
    private JTextArea VMoptionsArea;
    private JPanel buttonGroup;
    private JButton runWithUnlogged;
    private JButton waitingForLogs;
    private JLabel headingLabel;
    private JLabel descriptionText;
    private CardActionListener listener;

    private boolean logsPresent = false;
    public Run_Component_Obv3(ProjectTypeInfo.RUN_TYPES defaultType, boolean logsPresent, CardActionListener listener)
    {
        this.listener = listener;
        this.headingLabel.setText("Run Unlogged with "+ UI_Utils.getDisplayNameForType(defaultType));
        this.headingLabel.setIcon(UI_Utils.getIconForRuntype(defaultType));
        setDescriptionText();
        if(logsPresent)
        {
            this.logsPresent = logsPresent;
            waitingForLogs.setText("Generate cases");
        }
        else
        {
            listener.checkForSelogs();
        }
        waitingForLogs.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleTransition();
            }
        });
    }

    void handleTransition()
    {
        if(logsPresent)
        {
            listener.loadLiveLiew();
        }
    }
    public void loadContentsForType(ProjectTypeInfo.RUN_TYPES type)
    {
        //switch and morph text to display in section
    }
    public JPanel getComponent()
    {
        return mainPanel;
    }

    public void setVMtext(String text)
    {
        this.VMoptionsArea.setText(text);
    }

    private void setDescriptionText()
    {
        this.descriptionText.setText("<html><body>" +
                "<p>Copy this command and run inside your terminal so that your application starts running with our agent.</p>" +
                "<p>Once you run the application with the agent, access your application from Postman, Swagger or UI and Unlogged will<br> be ready to start generating the unit tests.</p>" +
                "</body></html>");
    }
}
