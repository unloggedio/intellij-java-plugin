package com.insidious.plugin.ui.Components;

import com.insidious.plugin.pojo.ProjectTypeInfo;

import javax.swing.*;
import java.awt.*;

public class Obv3_RunType_Element {
    private JPanel mainPanel;
    private JPanel borderParent;
    private JPanel centerPanel;
    private JPanel rightIconContainer;
    private JLabel iconHolder;

    public Obv3_RunType_Element(ProjectTypeInfo.RUN_TYPES type)
    {
        this.iconHolder.setText(type.toString());
    }

    public void setContent(JRadioButton contentPanel)
    {
        this.centerPanel.removeAll();
        centerPanel.add(contentPanel, BorderLayout.CENTER);
        centerPanel.revalidate();
    }

    public JPanel getComponent()
    {
        return mainPanel;
    }

}
