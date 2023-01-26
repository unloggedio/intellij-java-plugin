package com.insidious.plugin.ui.Components;

import com.insidious.plugin.pojo.ProjectTypeInfo;

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
        this.headingLabel.setText("Run Unlogged with "+defaultType);
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
}
